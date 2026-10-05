package br.com.imoveisregla.client.feature.profile

import br.com.imoveisregla.core.model.ClientDocument
import br.com.imoveisregla.core.model.ClientProfile
import br.com.imoveisregla.core.model.ClientProfileInput
import br.com.imoveisregla.core.model.DocumentKind
import br.com.imoveisregla.core.model.EmploymentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileFormStateTest {

    private val mariana = ClientProfile(
        userId = "client-1", fullName = "Mariana Souza", email = "cliente@exemplo.com", phone = "11987654321",
        cpf = "52998224725", birthDate = "1992-04-18", occupation = "Designer",
        employmentType = EmploymentType.CLT, monthlyIncome = 18_000, residents = 2, hasPets = true,
    )

    @Test fun fromProfile_masksAndConvertsDate() {
        val s = ProfileFormState.from(mariana, "outro@exemplo.com")
        assertEquals("Mariana Souza", s.fullName)
        assertEquals("cliente@exemplo.com", s.email)
        assertEquals("(11) 98765-4321", s.phone)
        assertEquals("529.982.247-25", s.cpf)
        assertEquals("18/04/1992", s.birthDate)
        assertEquals("18000", s.monthlyIncome)
        assertEquals(EmploymentType.CLT, s.employmentType)
        assertEquals(2, s.residents)
        assertTrue(s.hasPets)
    }

    @Test fun roundTrip_toInput() {
        val input = ProfileFormState.from(mariana, null).toInput()
        assertEquals(
            ClientProfileInput(
                fullName = "Mariana Souza", email = "cliente@exemplo.com", phone = "(11) 98765-4321",
                cpf = "529.982.247-25", birthDate = "1992-04-18", occupation = "Designer",
                employmentType = EmploymentType.CLT, monthlyIncome = 18_000, residents = 2, hasPets = true,
            ),
            input,
        )
        assertTrue(ProfileFormState.from(mariana, null).validate(requireFinancials = true).isEmpty())
    }

    @Test fun fromNull_usesSessionEmail() {
        val s = ProfileFormState.from(null, "novo@exemplo.com")
        assertEquals("novo@exemplo.com", s.email)
        assertEquals("", s.fullName)
        assertEquals(1, s.residents)
        val input = s.toInput()
        assertNull(input.cpf)
        assertNull(input.birthDate)
        assertNull(input.occupation)
        assertNull(input.monthlyIncome)
    }

    @Test fun birthDateConversion() {
        assertEquals("18/04/1992", maskDateBR("18041992"))
        assertEquals("18/04", maskDateBR("1804"))
        assertEquals("1992-04-18", brDateToIso("18/04/1992"))
        assertNull(brDateToIso("  "))
        assertEquals("31/02/1992", brDateToIso("31/02/1992")) // impossible date stays invalid
        assertEquals("18/04/19", brDateToIso("18/04/19"))
        assertEquals("18/04/1992", isoDateToBr("1992-04-18"))
        assertEquals("", isoDateToBr(null))
        val errors = ProfileFormState.from(mariana, null).copy(birthDate = "31/02/1992").validate()
        assertEquals("Data inválida (DD/MM/AAAA)", errors["birthDate"])
    }

    @Test fun incomeFormatting() {
        assertEquals("R$ 18.000", formatIncomeInput("18000").replace(' ', ' '))
        assertEquals("", formatIncomeInput(""))
    }

    @Test fun invalidCpf_reported() {
        val errors = ProfileFormState.from(mariana, null).copy(cpf = "111.111.111-11").validate()
        assertEquals("CPF inválido", errors["cpf"])
    }

    @Test fun completionPercent() {
        assertEquals(0, profileCompletion(null))
        assertEquals(100, profileCompletion(mariana))
        val partial = mariana.copy(cpf = null, birthDate = null, occupation = null, employmentType = null, monthlyIncome = null)
        assertEquals(37, profileCompletion(partial)) // 3 of 8
        assertEquals(50, profileCompletion(partial.copy(cpf = "52998224725")))
    }

    @Test fun initialsFromNameOrEmail() {
        assertEquals("MS", initials("Mariana Souza"))
        assertEquals("CL", initials("cliente@exemplo.com"))
    }

    @Test fun requiredDocuments() {
        fun doc(id: Long, kind: DocumentKind) =
            ClientDocument(id, "client-1", null, kind, "f.pdf", "client-1/x/f.pdf", "application/pdf", 10)
        val docs = listOf(doc(1, DocumentKind.RG_CNH), doc(2, DocumentKind.CPF))
        assertEquals(
            mapOf(DocumentKind.RG_CNH to true, DocumentKind.COMPROVANTE_RENDA to false, DocumentKind.COMPROVANTE_RESIDENCIA to false),
            requiredDocumentsStatus(docs),
        )
        assertFalse(hasAllRequiredDocuments(docs))
        val all = docs + doc(3, DocumentKind.COMPROVANTE_RENDA) + doc(4, DocumentKind.COMPROVANTE_RESIDENCIA)
        assertTrue(hasAllRequiredDocuments(all))
        assertEquals(DocumentKind.entries.size, CHECKLIST_ORDER.size)
    }
}
