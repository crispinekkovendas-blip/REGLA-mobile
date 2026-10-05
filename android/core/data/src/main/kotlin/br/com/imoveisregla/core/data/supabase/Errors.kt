package br.com.imoveisregla.core.data.supabase

import br.com.imoveisregla.core.data.NotAuthenticatedException
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import kotlin.coroutines.cancellation.CancellationException

/** User-facing error raised by the Supabase repositories (message is pt-BR, cause is kept). */
class ReglaDataException(message: String, cause: Throwable? = null) : IllegalStateException(message, cause)

private const val OFFLINE = "Sem conexão com o servidor. Verifique sua internet e tente novamente."

/** Runs [block], translating supabase-kt / Ktor failures into pt-BR [ReglaDataException]s. */
internal suspend inline fun <T> remote(block: () -> T): T = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: NotAuthenticatedException) {
    throw e
} catch (e: ReglaDataException) {
    throw e
} catch (e: Throwable) {
    throw translate(e)
}

internal fun translate(e: Throwable): Throwable = when (e) {
    is AuthRestException -> ReglaDataException(authMessage(e.error, e.errorDescription), e)
    is PostgrestRestException -> ReglaDataException(postgrestMessage(e), e)
    is RestException -> when (e.statusCode) {
        401 -> ReglaDataException("Sua sessão expirou. Faça login novamente.", e)
        403 -> ReglaDataException("Você não tem permissão para esta ação.", e)
        404 -> ReglaDataException("Não encontrado.", e)
        else -> ReglaDataException("Erro no servidor (${e.statusCode}). Tente novamente.", e)
    }
    is HttpRequestException, is java.io.IOException -> ReglaDataException(OFFLINE, e)
    else -> e
}

private fun postgrestMessage(e: PostgrestRestException): String {
    val raw = e.message.orEmpty()
    return when {
        raw.contains("clients can only withdraw", ignoreCase = true) -> "Esta proposta não pode mais ser cancelada"
        e.code == "42501" || raw.contains("row-level security", ignoreCase = true) ->
            "Você não tem permissão para esta ação."
        e.code == "23505" -> "Este registro já existe."
        e.code == "23503" -> "Registro relacionado não encontrado."
        e.code == "23514" || e.code == "22P02" || e.code == "22007" || e.code == "22008" ->
            "Dados inválidos. Revise os campos e tente novamente."
        e.code == "PGRST116" -> "Não encontrado."
        e.code == "PGRST301" || e.code == "PGRST303" -> "Sua sessão expirou. Faça login novamente."
        else -> "Erro no servidor. Tente novamente."
    }
}

internal fun authMessage(code: String, description: String?): String = when (code) {
    "invalid_credentials", "invalid_grant" -> "E-mail ou senha incorretos"
    "email_not_confirmed" -> "Confirme seu e-mail antes de entrar"
    "user_already_exists", "email_exists" -> "Já existe uma conta com este e-mail"
    "weak_password" -> "A senha precisa de pelo menos 6 caracteres"
    "over_request_rate_limit", "over_email_send_rate_limit" -> "Muitas tentativas. Aguarde alguns minutos."
    "email_address_invalid", "validation_failed" -> "E-mail inválido"
    "signup_disabled" -> "Cadastros estão temporariamente desativados"
    else -> description?.takeIf { it.isNotBlank() }?.let { "Falha na autenticação: $it" } ?: "Falha na autenticação"
}
