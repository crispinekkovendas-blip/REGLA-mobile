package br.com.imoveisregla.core.data.supabase

import br.com.imoveisregla.core.data.NotAuthenticatedException
import br.com.imoveisregla.core.data.expectThrows
import br.com.imoveisregla.core.model.ApplicationInput
import br.com.imoveisregla.core.model.ApplicationIntent
import br.com.imoveisregla.core.model.GuaranteeType
import br.com.imoveisregla.core.model.InquiryStage
import br.com.imoveisregla.core.model.ListingFilters
import br.com.imoveisregla.core.model.ListingType
import br.com.imoveisregla.core.model.Priority
import br.com.imoveisregla.core.model.VisitInput
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.resumable.MemoryResumableCache
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.concurrent.CopyOnWriteArrayList

/** Repositories against a Ktor MockEngine: asserts the PostgREST / Storage requests they make. */
class SupabaseRepositoryTest {

    data class Call(val method: HttpMethod, val url: Url, val headers: Headers, val body: String) {
        val path: String get() = url.encodedPath
        fun param(name: String): String? = url.parameters[name]
        val jsonBody: JsonObject get() = ReglaJson.parseToJsonElement(body).let { if (it is JsonArray) it[0].jsonObject else it.jsonObject }
        val prefer: String get() = headers.getAll("Prefer").orEmpty().joinToString(",")
    }

    private val calls = CopyOnWriteArrayList<Call>()
    private var reply: (Call) -> String = { "[]" }

    private val client: SupabaseClient = createSupabaseClient("https://test.supabase.co", "anon-key") {
        osInformation = null
        defaultSerializer = KotlinXSerializer(ReglaJson)
        httpEngine = MockEngine { req ->
            val call = Call(req.method, req.url, req.headers, req.body.toByteArray().decodeToString())
            calls += call
            respond(
                content = if (req.method == HttpMethod.Head) "" else reply(call),
                status = HttpStatusCode.OK,
                headers = headersOf(
                    HttpHeaders.ContentType to listOf("application/json"),
                    HttpHeaders.ContentRange to listOf("0-0/7"),
                ),
            )
        }
        install(Postgrest)
        install(Storage) { resumable { cache = MemoryResumableCache() } }
    }

    private val user = CurrentUser { "u1" }
    private val nobody = CurrentUser { null }

    @After
    fun tearDown() {
        kotlinx.coroutines.runBlocking { client.close() }
    }

    @Test
    fun searchBuildsFilterQuery() = runTest {
        reply = { Fixtures.LISTINGS }
        val repo = SupabaseListingRepository(client, "https://test.supabase.co")
        val result = repo.search(
            ListingFilters(query = " Pinheiros, (SP) ", city = "São Paulo", type = ListingType.APARTMENT, minBeds = 2, maxPrice = 5_000),
        )
        val c = calls.single()
        assertEquals(HttpMethod.Get, c.method)
        assertEquals("/rest/v1/listings", c.path)
        assertEquals(Selects.LISTING, c.param("select"))
        assertEquals("eq.live", c.param("status"))
        assertEquals("eq.São Paulo", c.param("city"))
        assertEquals("eq.apartment", c.param("type"))
        assertEquals("gte.2", c.param("beds"))
        assertEquals("lte.5000", c.param("price"))
        val or = c.param("or")!!
        assertTrue(or, or.contains("title.ilike."))
        assertTrue(or, or.contains("neighborhood.ilike."))
        assertTrue(or, or.contains("city.ilike."))
        assertTrue(or, or.contains("%Pinheiros SP%"))
        assertEquals("featured.desc.nullslast,created_at.desc.nullslast", c.param("order"))
        assertEquals("100", c.param("limit"))
        // photos come back sorted by position
        assertEquals(listOf(0, 1), result.single().photos.map { it.position })
    }

    @Test
    fun searchWithoutFiltersOnlyRequiresLive() = runTest {
        SupabaseListingRepository(client, "https://test.supabase.co").search()
        val c = calls.single()
        assertEquals("eq.live", c.param("status"))
        assertEquals(null, c.param("or"))
        assertEquals(null, c.param("city"))
        assertEquals(null, c.param("beds"))
    }

    @Test
    fun photoUrlUsesPublicBucket() {
        val repo = SupabaseListingRepository(client, "https://test.supabase.co/")
        assertEquals("https://test.supabase.co/storage/v1/object/public/listing-photos/12/a.jpg", repo.photoUrl("12/a.jpg"))
        assertEquals("https://cdn.x/p.jpg", repo.photoUrl("https://cdn.x/p.jpg"))
    }

    @Test
    fun submitInsertsApplicationAfterProfileCheck() = runTest {
        reply = { c -> if (c.path.endsWith("client_profiles")) """[{"user_id":"u1"}]""" else Fixtures.APPLICATIONS }
        val repo = SupabaseApplicationRepository(client, user)
        val app = repo.submit(
            ApplicationInput(12, ApplicationIntent.RENT, 4_600, GuaranteeType.SEGURO_FIANCA, "2026-11-01", "Podemos fechar?"),
        )
        assertEquals(5L, app.id)
        assertEquals(88L, app.inquiryId)
        assertEquals(2, calls.size)
        val profileCheck = calls[0]
        assertEquals("/rest/v1/client_profiles", profileCheck.path)
        assertEquals("eq.u1", profileCheck.param("user_id"))

        val insert = calls[1]
        assertEquals(HttpMethod.Post, insert.method)
        assertEquals("/rest/v1/applications", insert.path)
        assertEquals(Selects.APPLICATION, insert.param("select"))
        assertTrue(insert.prefer, insert.prefer.contains("return=representation"))
        val body = insert.jsonBody
        assertEquals(
            setOf("listing_id", "user_id", "intent", "offered_price", "guarantee_type", "move_in_date", "message"),
            body.keys,
        )
        assertEquals("u1", body["user_id"]!!.jsonPrimitive.content)
        assertEquals("rent", body["intent"]!!.jsonPrimitive.content)
        assertEquals("seguro_fianca", body["guarantee_type"]!!.jsonPrimitive.content)
        assertEquals("4600", body["offered_price"]!!.jsonPrimitive.content)
    }

    @Test
    fun submitWithoutProfileFails() = runTest {
        reply = { "[]" }
        val e = expectThrows<ReglaDataException> {
            SupabaseApplicationRepository(client, user).submit(ApplicationInput(12, ApplicationIntent.RENT, 4_600))
        }
        assertTrue(e.message!!.contains("cadastro"))
        assertEquals(1, calls.size) // never reached the insert
    }

    @Test
    fun signedOutThrowsWithoutNetwork() = runTest {
        expectThrows<NotAuthenticatedException> { SupabaseFavoriteRepository(client, nobody).ids() }
        expectThrows<NotAuthenticatedException> { SupabaseProfileRepository(client, nobody).mine() }
        expectThrows<NotAuthenticatedException> {
            SupabaseVisitRepository(client, nobody).book(VisitInput(1, "2026-10-06T14:30:00-03:00", "Ana", "a@x.com"))
        }
        assertTrue(calls.isEmpty())
    }

    @Test
    fun withdrawPatchesOwnRowOnly() = runTest {
        reply = { """[{"id":5}]""" }
        SupabaseApplicationRepository(client, user).withdraw(5)
        val c = calls.single()
        assertEquals(HttpMethod.Patch, c.method)
        assertEquals("eq.5", c.param("id"))
        assertEquals("eq.u1", c.param("user_id"))
        assertEquals(setOf("status"), c.jsonBody.keys)
        assertEquals("withdrawn", c.jsonBody["status"]!!.jsonPrimitive.content)
    }

    @Test
    fun withdrawNoRowsFails() = runTest {
        reply = { "[]" }
        expectThrows<ReglaDataException> { SupabaseApplicationRepository(client, user).withdraw(5) }
    }

    @Test
    fun reviewForReviewEmbedsProfiles() = runTest {
        reply = { Fixtures.APPLICATIONS_REVIEW }
        val rows = SupabaseApplicationRepository(client, user).forReview(null)
        assertEquals("Mariana Souza", rows.single().profile?.fullName)
        assertEquals(Selects.APPLICATION_REVIEW, calls.single().param("select"))
        assertTrue(Selects.APPLICATION_REVIEW.contains("client_profiles!applications_user_id_fkey(*)"))
    }

    @Test
    fun bookVisitInsertsPrivateScheduledShowing() = runTest {
        SupabaseVisitRepository(client, user).book(
            VisitInput(12, "2026-10-06T14:30:00-03:00", " Ana ", "ana@x.com", "", "Prefiro à tarde"),
        )
        val c = calls.single()
        assertEquals(HttpMethod.Post, c.method)
        assertEquals("/rest/v1/showings", c.path)
        val b = c.jsonBody
        assertEquals("private", b["type"]!!.jsonPrimitive.content)
        assertEquals("scheduled", b["status"]!!.jsonPrimitive.content)
        assertEquals("u1", b["created_by"]!!.jsonPrimitive.content)
        assertEquals("Ana", b["visitor_name"]!!.jsonPrimitive.content)
        assertEquals("2026-10-06T14:30:00-03:00", b["starts_at"]!!.jsonPrimitive.content)
        assertFalse("id" in b)
    }

    @Test
    fun favoritesUpsertAndDelete() = runTest {
        val repo = SupabaseFavoriteRepository(client, user)
        repo.set(12, true)
        repo.set(12, false)
        val (add, del) = calls
        assertEquals(HttpMethod.Post, add.method)
        assertEquals("/rest/v1/favorites", add.path)
        assertEquals("user_id,listing_id", add.param("on_conflict"))
        assertTrue(add.prefer, add.prefer.contains("resolution=ignore-duplicates"))
        assertEquals("u1", add.jsonBody["user_id"]!!.jsonPrimitive.content)
        assertEquals(HttpMethod.Delete, del.method)
        assertEquals("eq.u1", del.param("user_id"))
        assertEquals("eq.12", del.param("listing_id"))
    }

    @Test
    fun leadsListUsesFkHintAndStage() = runTest {
        reply = { Fixtures.INQUIRIES }
        val leads = SupabaseLeadRepository(client, user).list(InquiryStage.OFFER)
        assertEquals(2, leads.size)
        val c = calls.single()
        assertEquals("/rest/v1/inquiries", c.path)
        assertEquals("eq.offer", c.param("stage"))
        assertTrue(c.param("select")!!.contains("listings!inquiries_property_id_fkey("))
        assertEquals("last_activity_at.desc.nullslast", c.param("order"))
    }

    @Test
    fun leadUpdateSendsOnlyChangedFields() = runTest {
        reply = { """[{"id":88}]""" }
        val repo = SupabaseLeadRepository(client, user)
        repo.update(88, priority = Priority.LOW, read = true)
        repo.update(88) // nothing to change → no request
        val c = calls.single()
        assertEquals(setOf("priority", "read"), c.jsonBody.keys)
        assertEquals("low", c.jsonBody["priority"]!!.jsonPrimitive.content)
    }

    @Test
    fun addNoteSendsAuthor() = runTest {
        reply = { Fixtures.NOTES }
        val n = SupabaseLeadRepository(client, user).addNote(88, "  Ligar amanhã ")
        assertEquals("Ligar amanhã", n.body)
        val b = calls.single().jsonBody
        assertEquals(setOf("inquiry_id", "body", "author_id"), b.keys)
        assertEquals("Ligar amanhã", b["body"]!!.jsonPrimitive.content)
        assertEquals("u1", b["author_id"]!!.jsonPrimitive.content)
    }

    @Test
    fun agendaRangeFilters() = runTest {
        reply = { Fixtures.SHOWINGS }
        val rows = SupabaseAgendaRepository(client).range("2026-10-06T00:00-03:00", "2026-10-07T00:00-03:00")
        assertEquals(1, rows.size)
        val c = calls.single()
        assertEquals("/rest/v1/showings", c.path)
        assertRange(c, "2026-10-06T00:00-03:00", "2026-10-07T00:00-03:00")
        assertEquals("starts_at.asc.nullslast", c.param("order"))
    }

    private fun assertRange(c: Call, from: String, to: String) {
        assertEquals(null, c.param("starts_at"))
        val and = c.param("and")!!.replace("\"", "")
        assertEquals("(starts_at.gte.$from,starts_at.lt.$to)", and)
    }

    @Test
    fun dashboardCountsWithHeadRequests() = runTest {
        val stats = SupabaseDashboardRepository(client) { LocalDate.of(2026, 10, 6) }.stats()
        assertEquals(7, stats.newLeads)
        assertEquals(7, stats.pendingApplications)
        assertEquals(7, stats.visitsToday)
        assertEquals(7, stats.liveListings)
        assertEquals(4, calls.size)
        assertTrue(calls.all { it.method == HttpMethod.Head && it.prefer.contains("count=exact") })
        val byTable = calls.associateBy { it.path.substringAfterLast('/') }
        assertEquals("eq.inbox", byTable["inquiries"]!!.param("stage"))
        assertEquals("in.(submitted,under_review)", byTable["applications"]!!.param("status"))
        assertEquals("eq.live", byTable["listings"]!!.param("status"))
        assertRange(byTable["showings"]!!, "2026-10-06T00:00-03:00", "2026-10-07T00:00-03:00")
    }

    @Test
    fun documentsListForRealtorUsesGivenUser() = runTest {
        reply = { Fixtures.DOCUMENTS }
        val docs = SupabaseDocumentRepository(client, nobody).list("client-9")
        assertEquals(1, docs.size)
        assertEquals("eq.client-9", calls.single().param("user_id"))
    }

    @Test
    fun attachReinsertsAndDeletesOriginals() = runTest {
        reply = { c -> if (c.method == HttpMethod.Get) Fixtures.DOCUMENTS else "" }
        SupabaseDocumentRepository(client, user).attachToApplication(listOf(31), 5)
        assertEquals(listOf(HttpMethod.Get, HttpMethod.Post, HttpMethod.Delete), calls.map { it.method })
        val inserted = ReglaJson.parseToJsonElement(calls[1].body).jsonArray.single().jsonObject
        assertEquals("5", inserted["application_id"]!!.jsonPrimitive.content)
        assertEquals("u1/comprovante_renda/1759500000000-holerite_setembro.pdf", inserted["storage_path"]!!.jsonPrimitive.content)
        assertFalse("id" in inserted)
        assertEquals("in.(31)", calls[2].param("id"))
    }

    @Test
    fun signedUrlResolvesAgainstStorage() = runTest {
        reply = { """{"signedURL":"/object/sign/client-documents/u1/cpf/1-a.pdf?token=abc"}""" }
        val url = SupabaseDocumentRepository(client, user).signedUrl("u1/cpf/1-a.pdf", 120)
        val c = calls.single()
        assertEquals(HttpMethod.Post, c.method)
        assertEquals("/storage/v1/object/sign/client-documents/u1/cpf/1-a.pdf", c.path)
        assertEquals("120", c.jsonBody["expiresIn"]!!.jsonPrimitive.content)
        assertTrue(url, url.startsWith("https://test.supabase.co/storage/v1/object/sign/client-documents/u1/cpf/1-a.pdf"))
        assertTrue(url, url.contains("token=abc"))
    }
}
