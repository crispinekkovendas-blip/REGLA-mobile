package br.com.imoveisregla.core.data.google

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import br.com.imoveisregla.core.data.BuildConfig
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.security.MessageDigest
import java.security.SecureRandom

/** Google ID token + the raw nonce Supabase needs to verify it. */
data class GoogleIdToken(val idToken: String, val rawNonce: String)

/** Thrown with a pt-BR message the login screens can show as-is. */
class GoogleSignInException(message: String, cause: Throwable? = null) : IllegalStateException(message, cause)

/**
 * "Continuar com o Google" via Android Credential Manager (native account picker).
 *
 * Needs, in Google Cloud: a *Web* OAuth client (its id = GOOGLE_WEB_CLIENT_ID, also pasted
 * into Supabase → Auth → Google) and an *Android* OAuth client per app package
 * (br.com.imoveisregla.client / .realtor) with the REGLA signing key's SHA-1.
 */
object GoogleSignIn {
    val webClientId: String = BuildConfig.GOOGLE_WEB_CLIENT_ID

    val isConfigured: Boolean get() = webClientId.isNotBlank()

    /**
     * Shows the Google account picker. Returns null when the user cancels.
     * Must be called with an Activity context.
     */
    suspend fun requestIdToken(context: Context): GoogleIdToken? {
        if (!isConfigured) {
            throw GoogleSignInException("Login com Google ainda não está disponível. Use e-mail e senha por enquanto.")
        }
        val rawNonce = randomNonce()
        val option = GetSignInWithGoogleOption.Builder(webClientId)
            .setNonce(sha256(rawNonce))
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val result = try {
            CredentialManager.create(context).getCredential(context, request)
        } catch (e: GetCredentialCancellationException) {
            return null
        } catch (e: NoCredentialException) {
            throw GoogleSignInException("Nenhuma conta Google encontrada neste aparelho.", e)
        } catch (e: GetCredentialException) {
            throw GoogleSignInException("Não foi possível entrar com o Google. Tente novamente.", e)
        }
        val credential = result.credential
        if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            throw GoogleSignInException("Resposta inesperada do Google. Tente novamente.")
        }
        val google = GoogleIdTokenCredential.createFrom(credential.data)
        return GoogleIdToken(google.idToken, rawNonce)
    }

    private fun randomNonce(): String {
        val bytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
}
