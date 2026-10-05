package br.com.imoveisregla.realtor.feature.leads

import br.com.imoveisregla.core.model.Inquiry
import br.com.imoveisregla.core.model.InquiryStage
import br.com.imoveisregla.core.model.Showing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.OffsetDateTime

class LeadsLogicTest {
    private val now = OffsetDateTime.parse("2026-10-04T12:00:00-03:00")

    private fun lead(id: Long, stage: InquiryStage, name: String = "Lead $id", msg: String = "Oi") =
        Inquiry(id = id, name = name, email = "l$id@exemplo.com", message = msg, stage = stage)

    @Test fun stageCountsIncludesEveryStage() {
        val counts = stageCounts(
            listOf(lead(1, InquiryStage.INBOX), lead(2, InquiryStage.INBOX), lead(3, InquiryStage.OFFER)),
        )
        assertEquals(InquiryStage.entries.size, counts.size)
        assertEquals(2, counts[InquiryStage.INBOX])
        assertEquals(1, counts[InquiryStage.OFFER])
        assertEquals(0, counts[InquiryStage.CLOSED_LOST])
    }

    @Test fun stageCountsEmpty() {
        assertEquals(0, stageCounts(emptyList()).values.sum())
    }

    @Test fun filterByStageAndQuery() {
        val all = listOf(
            lead(1, InquiryStage.INBOX, "Carlos Lima", "Ainda disponível?"),
            lead(2, InquiryStage.INBOX, "Fernanda", "Aceita PETS?"),
            lead(3, InquiryStage.SHOWING, "Juliana", "Visita amanhã"),
        )
        assertEquals(listOf(1L, 2L), filterLeads(all, InquiryStage.INBOX, "").map { it.id })
        assertEquals(listOf(2L), filterLeads(all, null, "pets").map { it.id })
        assertEquals(listOf(3L), filterLeads(all, null, "l3@exemplo").map { it.id })
        assertEquals(emptyList<Long>(), filterLeads(all, InquiryStage.SHOWING, "carlos").map { it.id })
    }

    @Test fun relativeTimeJustNow() {
        assertEquals("agora", relativeTime(now.minusSeconds(20).toString(), now))
    }

    @Test fun relativeTimeMinutes() {
        assertEquals("há 5 min", relativeTime(now.minusMinutes(5).toString(), now))
        assertEquals("há 59 min", relativeTime(now.minusMinutes(59).toString(), now))
    }

    @Test fun relativeTimeHours() {
        assertEquals("há 2 h", relativeTime(now.minusHours(2).toString(), now))
        assertEquals("há 23 h", relativeTime(now.minusHours(23).minusMinutes(30).toString(), now))
    }

    @Test fun relativeTimeDays() {
        assertEquals("há 1 dia", relativeTime(now.minusDays(1).toString(), now))
        assertEquals("há 5 dias", relativeTime(now.minusDays(5).toString(), now))
    }

    @Test fun relativeTimeOldFallsBackToDate() {
        assertEquals("04/08/2026", relativeTime("2026-08-04T12:00:00-03:00", now))
    }

    @Test fun relativeTimeOtherOffsetAndBadInput() {
        // 13:00 UTC == 10:00 BRT → just under 2 h before now
        assertEquals("há 1 h", relativeTime("2026-10-04T13:00:00.123456+00:00", now))
        assertEquals("", relativeTime("ontem", now))
    }

    @Test fun initialsFromName() {
        assertEquals("CL", initials("Carlos Lima"))
        assertEquals("MS", initials("  mariana de souza "))
        assertEquals("A", initials("ana"))
        assertEquals("?", initials(" "))
    }

    @Test fun phoneLookupMatchesEmailIgnoringCase() {
        val showings = listOf(
            Showing(id = 1, listingId = 1, startsAt = now.toString(), visitorEmail = "outro@x.com", visitorPhone = "(11) 1"),
            Showing(id = 2, listingId = 1, startsAt = now.toString(), visitorEmail = "Juliana@Exemplo.com", visitorPhone = null),
            Showing(id = 3, listingId = 1, startsAt = now.toString(), visitorEmail = "juliana@exemplo.com", visitorPhone = "(11) 91234-5678"),
        )
        assertEquals("(11) 91234-5678", phoneFromShowings("juliana@exemplo.com", showings))
        assertNull(phoneFromShowings("carlos@exemplo.com", showings))
    }
}
