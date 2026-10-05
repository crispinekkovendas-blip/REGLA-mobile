package br.com.imoveisregla.core.data.supabase

import br.com.imoveisregla.core.data.AgendaRepository
import br.com.imoveisregla.core.data.AppContainer
import br.com.imoveisregla.core.data.ApplicationRepository
import br.com.imoveisregla.core.data.AuthRepository
import br.com.imoveisregla.core.data.DashboardRepository
import br.com.imoveisregla.core.data.DocumentRepository
import br.com.imoveisregla.core.data.FavoriteRepository
import br.com.imoveisregla.core.data.LeadRepository
import br.com.imoveisregla.core.data.ListingRepository
import br.com.imoveisregla.core.data.ProfileRepository
import br.com.imoveisregla.core.data.VisitRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Builds the supabase-kt client used by the live apps (Auth + Postgrest + Storage + Realtime). */
internal fun createReglaSupabaseClient(url: String, anonKey: String): SupabaseClient =
    createSupabaseClient(supabaseUrl = url, supabaseKey = anonKey) {
        defaultSerializer = KotlinXSerializer(ReglaJson)
        install(Auth)
        install(Postgrest)
        install(Storage)
        install(Realtime)
    }

/** Supabase-backed container (schema: supabase/migrations 0001–0012). */
class SupabaseContainer(
    private val url: String,
    anonKey: String,
) : AppContainer {

    internal val client: SupabaseClient = createReglaSupabaseClient(url, anonKey)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val user = CurrentUser { client.auth.currentUserOrNull()?.id }

    override val isLive: Boolean = true
    override val auth: AuthRepository = SupabaseAuthRepository(client, scope)
    override val listings: ListingRepository = SupabaseListingRepository(client, url)
    override val favorites: FavoriteRepository = SupabaseFavoriteRepository(client, user)
    override val profiles: ProfileRepository = SupabaseProfileRepository(client, user)
    override val applications: ApplicationRepository = SupabaseApplicationRepository(client, user)
    override val documents: DocumentRepository = SupabaseDocumentRepository(client, user)
    override val visits: VisitRepository = SupabaseVisitRepository(client, user)
    override val leads: LeadRepository = SupabaseLeadRepository(client, user)
    override val agenda: AgendaRepository = SupabaseAgendaRepository(client)
    override val dashboard: DashboardRepository = SupabaseDashboardRepository(client)
}
