package br.com.imoveisregla.core.data.fake

import br.com.imoveisregla.core.model.Application
import br.com.imoveisregla.core.model.ApplicationIntent
import br.com.imoveisregla.core.model.ApplicationStatus
import br.com.imoveisregla.core.model.ClientProfile
import br.com.imoveisregla.core.model.Currency
import br.com.imoveisregla.core.model.EmploymentType
import br.com.imoveisregla.core.model.GuaranteeType
import br.com.imoveisregla.core.model.Inquiry
import br.com.imoveisregla.core.model.InquiryStage
import br.com.imoveisregla.core.model.LeadNote
import br.com.imoveisregla.core.model.Listing
import br.com.imoveisregla.core.model.Offer
import br.com.imoveisregla.core.model.Party
import br.com.imoveisregla.core.model.ListingPhoto
import br.com.imoveisregla.core.model.ListingStatus
import br.com.imoveisregla.core.model.ListingType
import br.com.imoveisregla.core.model.Priority
import br.com.imoveisregla.core.model.Showing
import br.com.imoveisregla.core.model.ShowingStatus
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/** Demo dataset: São Paulo rentals + sales, leads, visits and one proposal in review. */
object SampleData {
    private val BRT = ZoneOffset.ofHours(-3)

    private fun photos(listingId: Long, n: Int) = (0 until n).map {
        ListingPhoto(listingId * 10 + it, listingId, "https://picsum.photos/seed/regla$listingId-$it/1200/800", null, it)
    }

    private fun listing(
        id: Long, title: String, hood: String, type: ListingType, price: Long, beds: Int, baths: Int, area: Int,
        tags: List<String>, summary: String, featured: Boolean = false, status: ListingStatus = ListingStatus.LIVE,
    ) = Listing(
        id = id, slug = "demo-$id", title = title, city = "São Paulo", country = "Brazil", neighborhood = hood,
        type = type, price = price, currency = Currency.BRL, beds = beds, baths = baths, areaM2 = area, tags = tags,
        summary = summary,
        description = "$summary Condomínio com portaria 24h, próximo ao metrô, comércio e escolas. " +
            "Aceita pets. Documentação em dia, pronto para morar.",
        status = status, featured = featured,
        createdAt = OffsetDateTime.now(BRT).minusDays(id).toString(),
        updatedAt = OffsetDateTime.now(BRT).minusHours(id).toString(),
        photos = photos(id, 3),
    )

    fun seed(db: FakeBackend) {
        db.listingRows += listOf(
            listing(1, "Apartamento com varanda em Pinheiros", "Pinheiros", ListingType.APARTMENT, 4_800, 2, 2, 72,
                listOf("Aluguel", "Novo"), "Dois dormitórios com varanda gourmet e vaga coberta.", featured = true),
            listing(2, "Studio mobiliado na Vila Madalena", "Vila Madalena", ListingType.APARTMENT, 3_200, 1, 1, 38,
                listOf("Aluguel", "Mobiliado"), "Studio compacto e iluminado, totalmente mobiliado."),
            listing(3, "Casa térrea no Alto da Lapa", "Alto da Lapa", ListingType.HOUSE, 9_500, 3, 3, 210,
                listOf("Aluguel"), "Casa com quintal, churrasqueira e duas vagas.", featured = true),
            listing(4, "Cobertura duplex no Itaim Bibi", "Itaim Bibi", ListingType.APARTMENT, 3_950_000, 3, 4, 245,
                listOf("Venda", "Destaque"), "Cobertura com piscina privativa e vista para o skyline."),
            listing(5, "Apartamento perto do metrô Vila Mariana", "Vila Mariana", ListingType.APARTMENT, 3_900, 2, 1, 64,
                listOf("Aluguel"), "Dois quartos a 300 m da estação Vila Mariana."),
            listing(6, "Sala comercial na Av. Paulista", "Bela Vista", ListingType.COMMERCIAL, 7_200, 0, 2, 95,
                listOf("Aluguel", "Comercial"), "Conjunto comercial com recepção e duas salas."),
            listing(7, "Sobrado em Moema", "Moema", ListingType.HOUSE, 2_650_000, 4, 4, 280,
                listOf("Venda"), "Sobrado reformado a duas quadras do Parque Ibirapuera."),
            listing(8, "Loft industrial na Barra Funda", "Barra Funda", ListingType.APARTMENT, 4_100, 1, 1, 70,
                listOf("Aluguel"), "Loft com pé-direito duplo.", status = ListingStatus.DRAFT),
        )

        db.profileRows["client-1"] = ClientProfile(
            userId = "client-1", fullName = "Mariana Souza", email = "cliente@exemplo.com", phone = "(11) 98765-4321",
            cpf = "529.982.247-25", birthDate = "1992-04-18", occupation = "Designer",
            employmentType = EmploymentType.CLT, monthlyIncome = 18_000, residents = 2, hasPets = true,
            createdAt = OffsetDateTime.now(BRT).minusDays(3).toString(),
        )

        val now = OffsetDateTime.now(BRT).truncatedTo(ChronoUnit.HOURS)
        fun lead(id: Long, name: String, email: String, msg: String, listingId: Long?, stage: InquiryStage, p: Priority, read: Boolean, hoursAgo: Long) =
            Inquiry(
                id = id, name = name, email = email, message = msg, propertyId = listingId, stage = stage, priority = p,
                read = read, intent = "rent", lastActivityAt = now.minusHours(hoursAgo).toString(),
                createdAt = now.minusHours(hoursAgo + 2).toString(), listings = listingId?.let { db.listingRef(it) },
            )
        db.inquiryRows += listOf(
            lead(101, "Carlos Lima", "carlos@exemplo.com", "Olá! O apartamento de Pinheiros ainda está disponível?", 1, InquiryStage.INBOX, Priority.HIGH, false, 1),
            lead(102, "Fernanda Alves", "fernanda@exemplo.com", "Gostaria de saber se aceita pets de grande porte.", 3, InquiryStage.INBOX, Priority.MEDIUM, false, 4),
            lead(103, "Rafael Torres", "rafael@exemplo.com", "Quero agendar uma visita no sábado.", 5, InquiryStage.QUALIFIED, Priority.MEDIUM, true, 20),
            lead(104, "Juliana Prado", "juliana@exemplo.com", "Visita marcada para amanhã às 10h.", 2, InquiryStage.SHOWING, Priority.LOW, true, 30),
            lead(105, "Mariana Souza", "cliente@exemplo.com", "Proposta via app: aluguel de R$ 4.600 · seguro fiança", 1, InquiryStage.OFFER, Priority.HIGH, false, 6),
            lead(106, "Bruno Costa", "bruno@exemplo.com", "Contrato assinado, obrigado!", 6, InquiryStage.CLOSED_WON, Priority.MEDIUM, true, 120),
        )
        db.noteRows += LeadNote(901, 103, "Prefere visitas aos sábados pela manhã.", "realtor-1", now.minusHours(19).toString())

        fun showing(id: Long, listingId: Long, at: OffsetDateTime, name: String, status: ShowingStatus, by: String? = null) =
            Showing(
                id = id, listingId = listingId, startsAt = at.toString(), status = status, visitorName = name,
                visitorEmail = "${name.substringBefore(' ').lowercase()}@exemplo.com", visitorPhone = "(11) 91234-5678",
                createdBy = by, listings = db.listingRef(listingId),
            )
        val today10 = now.withHour(10)
        db.showingRows += listOf(
            showing(201, 2, today10.plusDays(1), "Juliana Prado", ShowingStatus.CONFIRMED),
            showing(202, 5, today10.withHour(15), "Rafael Torres", ShowingStatus.SCHEDULED),
            showing(203, 1, today10.plusDays(2).withHour(11), "Mariana Souza", ShowingStatus.SCHEDULED, by = "client-1"),
            showing(204, 3, today10.minusDays(2), "Fernanda Alves", ShowingStatus.ATTENDED),
        )

        db.applicationRows += Application(
            id = 301, listingId = 1, userId = "client-1", intent = ApplicationIntent.RENT, offeredPrice = 4_600,
            guaranteeType = GuaranteeType.SEGURO_FIANCA, moveInDate = now.plusDays(20).toLocalDate().toString(),
            message = "Podemos fechar em 30 meses?", status = ApplicationStatus.UNDER_REVIEW, inquiryId = 105,
            createdAt = now.minusHours(6).toString(), updatedAt = now.minusHours(2).toString(),
            awaiting = Party.REALTOR,
        )
        db.offerRows += Offer(
            id = 3011, applicationId = 301, author = Party.CLIENT, authorId = "client-1", price = 4_600,
            guaranteeType = GuaranteeType.SEGURO_FIANCA, moveInDate = now.plusDays(20).toLocalDate().toString(),
            message = "Podemos fechar em 30 meses?", createdAt = now.minusHours(6).toString(),
        )

        // A live negotiation where the owner countered and it is the client's turn.
        db.applicationRows += Application(
            id = 302, listingId = 5, userId = "client-1", intent = ApplicationIntent.RENT, offeredPrice = 3_600,
            guaranteeType = GuaranteeType.CAUCAO, moveInDate = now.plusDays(30).toLocalDate().toString(),
            message = "Posso pagar 3 meses de caução.", status = ApplicationStatus.NEGOTIATING,
            createdAt = now.minusDays(1).toString(), updatedAt = now.minusHours(3).toString(),
            awaiting = Party.CLIENT,
        )
        db.offerRows += listOf(
            Offer(
                id = 3021, applicationId = 302, author = Party.CLIENT, authorId = "client-1", price = 3_600,
                guaranteeType = GuaranteeType.CAUCAO, moveInDate = now.plusDays(30).toLocalDate().toString(),
                message = "Posso pagar 3 meses de caução.", createdAt = now.minusDays(1).toString(),
            ),
            Offer(
                id = 3022, applicationId = 302, author = Party.REALTOR, authorId = "realtor-1", price = 3_800,
                guaranteeType = GuaranteeType.CAUCAO, moveInDate = now.plusDays(30).toLocalDate().toString(),
                message = "O proprietário aceita caução, mas pede R$ 3.800.", createdAt = now.minusHours(3).toString(),
            ),
        )
    }
}
