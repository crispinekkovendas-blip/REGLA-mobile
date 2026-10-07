package br.com.imoveisregla.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidationTest {
    private val okProfile = ClientProfileInput(
        fullName = "Maria da Silva",
        email = "maria@exemplo.com.br",
        phone = "(11) 93221-0855",
        cpf = "529.982.247-25",
        birthDate = "1990-05-17",
        employmentType = EmploymentType.CLT,
        monthlyIncome = 12_000,
        residents = 2,
    )

    @Test fun profile_valid() {
        assertEquals(emptyMap<String, String>(), validateProfile(okProfile))
        assertEquals(emptyMap<String, String>(), validateProfile(okProfile, requireFinancials = true))
    }

    @Test fun profile_minimalValidWithoutFinancials() {
        val p = ClientProfileInput(fullName = "Ana", email = "a@b.co", phone = "1132210855")
        assertEquals(emptyMap<String, String>(), validateProfile(p))
    }

    @Test fun profile_basicFieldErrors() {
        val errs = validateProfile(okProfile.copy(fullName = "  Al ", email = "maria@exemplo", phone = "1234-567"))
        assertEquals("Informe seu nome completo", errs["fullName"])
        assertEquals("E-mail inválido", errs["email"])
        assertEquals("Telefone inválido", errs["phone"])
        assertEquals(3, errs.size)
    }

    @Test fun profile_emailTrimmedAndSpacesRejected() {
        assertFalse(validateProfile(okProfile.copy(email = "  maria@ex.com  ")).containsKey("email"))
        assertTrue(validateProfile(okProfile.copy(email = "ma ria@ex.com")).containsKey("email"))
        assertTrue(validateProfile(okProfile.copy(email = "")).containsKey("email"))
    }

    @Test fun profile_cpf() {
        assertEquals("CPF inválido", validateProfile(okProfile.copy(cpf = "529.982.247-24"))["cpf"])
        assertFalse(validateProfile(okProfile.copy(cpf = null)).containsKey("cpf"))
        assertFalse(validateProfile(okProfile.copy(cpf = "  ")).containsKey("cpf"))
        assertEquals("Informe seu CPF", validateProfile(okProfile.copy(cpf = ""), requireFinancials = true)["cpf"])
    }

    @Test fun profile_birthDateFormat() {
        assertEquals("Use AAAA-MM-DD", validateProfile(okProfile.copy(birthDate = "17/05/1990"))["birthDate"])
        assertFalse(validateProfile(okProfile.copy(birthDate = "")).containsKey("birthDate"))
        assertFalse(validateProfile(okProfile.copy(birthDate = null)).containsKey("birthDate"))
    }

    @Test fun profile_incomeAndEmployment() {
        assertEquals("Renda inválida", validateProfile(okProfile.copy(monthlyIncome = -1))["monthlyIncome"])
        val fin = validateProfile(okProfile.copy(monthlyIncome = null, employmentType = null), requireFinancials = true)
        assertEquals("Informe sua renda mensal", fin["monthlyIncome"])
        // proposta mode needs CPF + income only; vínculo is optional (asked with the documents)
        assertEquals(setOf("monthlyIncome"), fin.keys)
        assertEquals("Informe seu CPF", validateProfile(okProfile.copy(cpf = null), requireFinancials = true)["cpf"])
        assertEquals("Informe sua renda mensal", validateProfile(okProfile.copy(monthlyIncome = 0), requireFinancials = true)["monthlyIncome"])
        // not required → missing financials are fine
        assertTrue(validateProfile(okProfile.copy(monthlyIncome = null, employmentType = null)).isEmpty())
    }

    @Test fun profile_residentsRange() {
        assertEquals("Entre 1 e 20 moradores", validateProfile(okProfile.copy(residents = 0))["residents"])
        assertTrue(validateProfile(okProfile.copy(residents = 21)).containsKey("residents"))
        assertFalse(validateProfile(okProfile.copy(residents = 1)).containsKey("residents"))
        assertFalse(validateProfile(okProfile.copy(residents = 20)).containsKey("residents"))
    }

    // ─── Application ─────────────────────────────────────────────────────

    private val okApp = ApplicationInput(
        listingId = 1, intent = ApplicationIntent.RENT, offeredPrice = 4_500,
        guaranteeType = GuaranteeType.SEGURO_FIANCA, moveInDate = "2026-11-01", message = "Tenho interesse.",
    )

    @Test fun application_valid() {
        assertTrue(validateApplication(okApp).isEmpty())
        assertTrue(validateApplication(okApp.copy(moveInDate = null, message = null)).isEmpty())
        assertTrue(validateApplication(okApp.copy(message = "x".repeat(2000))).isEmpty())
    }

    @Test fun application_errors() {
        assertEquals("Informe um valor", validateApplication(okApp.copy(offeredPrice = 0))["offeredPrice"])
        assertTrue(validateApplication(okApp.copy(offeredPrice = -10)).containsKey("offeredPrice"))
        assertEquals("Use AAAA-MM-DD", validateApplication(okApp.copy(moveInDate = "01/11/2026"))["moveInDate"])
        assertEquals("Máximo de 2000 caracteres", validateApplication(okApp.copy(message = "x".repeat(2001)))["message"])
    }

    // ─── Visit ───────────────────────────────────────────────────────────

    private val okVisit = VisitInput(
        listingId = 1, startsAt = "2026-10-06T14:30:00-03:00",
        visitorName = "João", visitorEmail = "joao@ex.com", visitorPhone = "11932210855",
    )

    @Test fun visit_valid() = assertTrue(validateVisit(okVisit).isEmpty())

    @Test fun visit_errors() {
        val e = validateVisit(okVisit.copy(visitorName = " J ", visitorEmail = "joao", startsAt = "  "))
        assertEquals("Informe seu nome", e["visitorName"])
        assertEquals("E-mail inválido", e["visitorEmail"])
        assertEquals("Escolha um horário", e["startsAt"])
        assertEquals(3, e.size)
    }
}
