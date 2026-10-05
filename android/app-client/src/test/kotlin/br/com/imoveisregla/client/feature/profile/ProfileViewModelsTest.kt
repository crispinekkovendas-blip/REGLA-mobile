package br.com.imoveisregla.client.feature.profile

import br.com.imoveisregla.core.data.SessionState
import br.com.imoveisregla.core.data.fake.FakeBackend
import br.com.imoveisregla.core.model.DocumentKind
import br.com.imoveisregla.core.model.EmploymentType
import br.com.imoveisregla.core.model.UploadFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelsTest {

    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun editProfile_loadsSeededProfile() {
        val backend = FakeBackend(asRealtor = false)
        val vm = EditProfileViewModel(backend)
        val s = vm.state.value
        assertFalse(s.loading)
        assertEquals("Mariana Souza", s.form.fullName)
        assertEquals("18/04/1992", s.form.birthDate)
    }

    @Test fun editProfile_invalidCpf_returnsErrorAndDoesNotSave() {
        val backend = FakeBackend(asRealtor = false)
        val vm = EditProfileViewModel(backend)
        vm.onChange(vm.state.value.form.copy(cpf = "123.456.789-00"))
        vm.save()
        assertEquals("CPF inválido", vm.state.value.errors["cpf"])
        assertEquals("529.982.247-25", backend.profileRows["client-1"]?.cpf)
    }

    @Test fun editProfile_validSave_persists() {
        val backend = FakeBackend(asRealtor = false, seed = false)
        val vm = EditProfileViewModel(backend)
        assertEquals("cliente@exemplo.com", vm.state.value.form.email)
        vm.onChange(
            vm.state.value.form.copy(
                fullName = "João Pereira", phone = "(11) 91234-5678", cpf = "529.982.247-25",
                birthDate = "01/02/1990", occupation = "Engenheiro", employmentType = EmploymentType.SERVIDOR,
                monthlyIncome = "12500", residents = 3, hasPets = false,
            ),
        )
        vm.save()
        val row = backend.profileRows["client-1"]!!
        assertEquals("João Pereira", row.fullName)
        assertEquals("1990-02-01", row.birthDate)
        assertEquals(12_500L, row.monthlyIncome)
        assertEquals(EmploymentType.SERVIDOR, row.employmentType)
        assertEquals(3, row.residents)
        assertTrue(vm.state.value.errors.isEmpty())
        assertEquals("Cadastro salvo", vm.state.value.message)
        vm.consumeMessage()
        assertNull(vm.state.value.message)
    }

    @Test fun profile_completionAndSignOut() = runTest {
        val backend = FakeBackend(asRealtor = false)
        val vm = ProfileViewModel(backend)
        assertEquals("Mariana Souza", vm.state.value.profile?.fullName)
        assertEquals(100, vm.state.value.completion)
        vm.signOut()
        assertTrue(vm.state.value.session is SessionState.SignedOut)
        assertNull(vm.state.value.profile)
    }

    @Test fun profile_partialCompletion() {
        val backend = FakeBackend(asRealtor = false)
        backend.profileRows["client-1"] = backend.profileRows["client-1"]!!.copy(cpf = null, monthlyIncome = null)
        val vm = ProfileViewModel(backend)
        assertEquals(75, vm.state.value.completion)
    }

    @Test fun documents_uploadAddsRowAndUpdatesRequiredStatus() {
        val backend = FakeBackend(asRealtor = false)
        val vm = DocumentsViewModel(backend)
        assertFalse(vm.state.value.allRequiredDone)
        assertEquals(0, vm.state.value.requiredDone)

        vm.upload(DocumentKind.RG_CNH, UploadFile("rg.pdf", "application/pdf", ByteArray(100)))
        assertEquals(1, backend.documentRows.size)
        assertEquals(DocumentKind.RG_CNH, backend.documentRows[0].kind)
        assertEquals("rg.pdf", backend.documentRows[0].filename)
        assertEquals(1, vm.state.value.documents.size)
        assertNull(vm.state.value.uploadingKind)
        assertEquals(true, vm.state.value.requiredStatus[DocumentKind.RG_CNH])
        assertEquals(false, vm.state.value.requiredStatus[DocumentKind.COMPROVANTE_RENDA])

        vm.upload(DocumentKind.COMPROVANTE_RENDA, UploadFile("holerite.png", "image/png", ByteArray(10)))
        vm.upload(DocumentKind.COMPROVANTE_RESIDENCIA, UploadFile("conta.pdf", "application/pdf", ByteArray(10)))
        assertTrue(vm.state.value.allRequiredDone)
        assertEquals(3, vm.state.value.requiredDone)
    }

    @Test fun documents_tooLargeRejected_andDelete() {
        val backend = FakeBackend(asRealtor = false)
        val vm = DocumentsViewModel(backend)
        vm.upload(DocumentKind.OUTRO, UploadFile("big.pdf", "application/pdf", ByteArray((MAX_DOCUMENT_BYTES + 1).toInt())))
        assertTrue(backend.documentRows.isEmpty())
        assertEquals("O arquivo deve ter no máximo 10 MB", vm.state.value.message)

        vm.upload(DocumentKind.CPF, UploadFile("cpf.pdf", "application/pdf", ByteArray(5)))
        val id = backend.documentRows.single().id
        vm.delete(id)
        assertTrue(backend.documentRows.isEmpty())
        assertTrue(vm.state.value.documents.isEmpty())
    }

    @Test fun documents_openResolvesSignedUrl() {
        val backend = FakeBackend(asRealtor = false)
        val vm = DocumentsViewModel(backend)
        vm.upload(DocumentKind.RG_CNH, UploadFile("rg.pdf", "application/pdf", ByteArray(5)))
        var opened: String? = null
        vm.open(vm.state.value.documents.first()) { opened = it }
        assertTrue(opened!!.contains("client-documents"))
    }
}
