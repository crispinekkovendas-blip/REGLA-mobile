package br.com.imoveisregla.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatTest {
    // ─── formatPrice / formatArea / listingRef ──────────────────────────

    @Test fun formatPrice_brl() {
        assertEquals("R$ 1.850.000", formatPrice(1_850_000, Currency.BRL))
        assertEquals("R$ 4.500", formatPrice(4_500, Currency.BRL))
        assertEquals("R$ 0", formatPrice(0, Currency.BRL))
        assertEquals("R$ 999", formatPrice(999, Currency.BRL))
    }

    @Test fun formatPrice_hasNoNonBreakingSpacesOrDecimals() {
        val s = formatPrice(1_234_567, Currency.BRL)
        assertFalse(s.contains(' '))
        assertFalse(s.contains(' '))
        assertFalse(s.contains(','))
    }

    @Test fun formatPrice_usd() {
        assertEquals("US$ 1.850.000", formatPrice(1_850_000, Currency.USD))
        assertEquals("$1,850,000", formatPrice(1_850_000, Currency.USD, java.util.Locale.US))
    }

    @Test fun formatArea_groupsThousands() {
        assertEquals("68 m²", formatArea(68))
        assertEquals("1.200 m²", formatArea(1200))
        assertEquals("0 m²", formatArea(0))
    }

    @Test fun listingRef_padsToFour() {
        assertEquals("RG-0042", listingRef(42))
        assertEquals("RG-0000", listingRef(0))
        assertEquals("RG-1234", listingRef(1234))
        assertEquals("RG-12345", listingRef(12345))
    }

    @Test fun digitsOnly_strips() {
        assertEquals("52998224725", digitsOnly("529.982.247-25"))
        assertEquals("", digitsOnly("abc"))
    }

    // ─── CPF ─────────────────────────────────────────────────────────────

    @Test fun cpf_valid() {
        assertTrue(isValidCpf("529.982.247-25"))
        assertTrue(isValidCpf("52998224725"))
        assertTrue(isValidCpf("111.444.777-35"))
        assertTrue(isValidCpf("11144477735"))
    }

    @Test fun cpf_invalidCheckDigits() {
        assertFalse(isValidCpf("529.982.247-24"))
        assertFalse(isValidCpf("529.982.247-15"))
        assertFalse(isValidCpf("111.444.777-36"))
    }

    @Test fun cpf_repeatedDigitsRejected() {
        for (d in '0'..'9') assertFalse("repeated $d", isValidCpf(d.toString().repeat(11)))
        assertFalse(isValidCpf("111.111.111-11"))
    }

    @Test fun cpf_wrongLength() {
        assertFalse(isValidCpf(""))
        assertFalse(isValidCpf("5299822472"))
        assertFalse(isValidCpf("529982247250"))
        assertFalse(isValidCpf("abc"))
    }

    @Test fun maskCpf_full() = assertEquals("529.982.247-25", maskCpf("52998224725"))

    @Test fun maskCpf_partial() {
        assertEquals("", maskCpf(""))
        assertEquals("529", maskCpf("529"))
        assertEquals("529.9", maskCpf("5299"))
        assertEquals("529.982", maskCpf("529982"))
        assertEquals("529.982.2", maskCpf("5299822"))
        assertEquals("529.982.247", maskCpf("529982247"))
        assertEquals("529.982.247-2", maskCpf("5299822472"))
    }

    @Test fun maskCpf_idempotentAndTruncates() {
        assertEquals("529.982.247-25", maskCpf("529.982.247-25"))
        assertEquals("529.982.247-25", maskCpf("529982247251234"))
        assertEquals("529", maskCpf("ab5c29"))
    }

    @Test fun maskPhone_full() {
        assertEquals("(11) 93221-0855", maskPhoneBR("11932210855"))
        assertEquals("(11) 3221-0855", maskPhoneBR("1132210855"))
        assertEquals("(11) 93221-0855", maskPhoneBR("(11) 93221-0855"))
    }

    @Test fun maskPhone_partial() {
        assertEquals("", maskPhoneBR(""))
        assertEquals("(1", maskPhoneBR("1"))
        assertEquals("(11", maskPhoneBR("11"))
        assertEquals("(11) 9", maskPhoneBR("119"))
        assertEquals("(11) 9322", maskPhoneBR("119322"))
        assertEquals("(11) 9-3221", maskPhoneBR("1193221"))
        assertEquals("(11) 93221-0855", maskPhoneBR("119322108559999"))
    }

    // ─── Affordability ───────────────────────────────────────────────────

    @Test fun affordability_thresholds() {
        assertEquals(Affordability.OK, affordability(2_000, 10_000))
        assertEquals(Affordability.OK, affordability(3_000, 10_000))      // exactly 30%
        assertEquals(Affordability.TIGHT, affordability(3_001, 10_000))
        assertEquals(Affordability.TIGHT, affordability(4_000, 10_000))   // exactly 40%
        assertEquals(Affordability.OVER, affordability(4_001, 10_000))
        assertEquals(Affordability.OVER, affordability(20_000, 10_000))
        assertEquals(Affordability.OK, affordability(0, 10_000))
    }

    @Test fun affordability_unknownIncome() {
        assertEquals(Affordability.UNKNOWN, affordability(3_000, null))
        assertEquals(Affordability.UNKNOWN, affordability(3_000, 0))
        assertEquals(Affordability.UNKNOWN, affordability(3_000, -5))
    }

    @Test fun affordability_labels() {
        assertEquals("Cabe no orçamento", Affordability.OK.label)
        assertEquals("Informe sua renda", Affordability.UNKNOWN.label)
    }

    // ─── documentPath ────────────────────────────────────────────────────

    @Test fun documentPath_sanitizesAccentsAndSpaces() {
        assertEquals(
            "user-1/comprovante_renda/1700000000000-Holerite_Marco_2026.pdf",
            documentPath("user-1", DocumentKind.COMPROVANTE_RENDA, "Holerite Março 2026.pdf", now = 1_700_000_000_000),
        )
        assertEquals(
            "u/rg_cnh/1-Joao_Conceicao_e_Sa.jpg",
            documentPath("u", DocumentKind.RG_CNH, "João Conceição é Sá.jpg", now = 1),
        )
    }

    @Test fun documentPath_neutralizesSlashesAndSymbols() {
        val p = documentPath("u", DocumentKind.OUTRO, "../../etc/passwd (1)#.txt", now = 5)
        assertEquals("u/outro/5-.._.._etc_passwd__1__.txt", p)
        assertEquals(3, p.split('/').size)
    }

    @Test fun documentPath_keepsSafeChars() {
        assertEquals("u/cpf/9-my-file_v2.PDF", documentPath("u", DocumentKind.CPF, "my-file_v2.PDF", now = 9))
    }

    // ─── WhatsApp ────────────────────────────────────────────────────────

    private fun textParam(url: String) =
        java.net.URLDecoder.decode(url.substringAfter("?text="), "UTF-8")

    @Test fun waInterest_encodesMessage() {
        val url = waInterest("Casa Verde & Jardim", "RG-0042")
        assertTrue(url.startsWith("https://wa.me/5511932210855?text="))
        val q = url.substringAfter("?text=")
        assertFalse("spaces must be %20, not +", q.contains('+'))
        assertFalse(q.contains(' '))
        assertTrue(q.contains("%20"))
        assertTrue(q.contains("%22Casa%20Verde%20%26%20Jardim%22"))
        assertEquals(
            "Olá! Tenho interesse no imóvel \"Casa Verde & Jardim\" (ref. RG-0042). Pode me passar mais informações?",
            textParam(url),
        )
    }

    @Test fun waToClient_prefixes55() {
        assertEquals("https://wa.me/5511932210855?text=Oi", waToClient("(11) 93221-0855", "Oi"))
        assertEquals("https://wa.me/551132210855?text=Oi", waToClient("11 3221-0855", "Oi"))
    }

    @Test fun waToClient_keepsExistingCountryCode() {
        assertTrue(waToClient("+55 11 93221-0855", "Oi").startsWith("https://wa.me/5511932210855?"))
        assertTrue(waToClient("5511932210855", "Oi").startsWith("https://wa.me/5511932210855?"))
    }

    @Test fun waToClient_encodesText() {
        val url = waToClient("11932210855", "Olá, tudo bem? 100%")
        assertEquals("Olá, tudo bem? 100%", textParam(url))
        assertFalse(url.substringAfter("?text=").contains('+'))
        assertTrue(url.contains("Ol%C3%A1"))
    }

    @Test fun waUrl_customPhone() {
        assertEquals("https://wa.me/123?text=a%20b", waUrl("a b", "123"))
    }
}
