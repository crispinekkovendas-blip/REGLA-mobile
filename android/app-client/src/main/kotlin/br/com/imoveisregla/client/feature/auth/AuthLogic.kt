package br.com.imoveisregla.client.feature.auth

import java.io.IOException

private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

internal const val MIN_PASSWORD_LENGTH = 6

internal fun isValidEmail(email: String): Boolean = EMAIL_REGEX.matches(email.trim())

/** Maps backend (Supabase GoTrue / fake) errors to friendly pt-BR messages. */
internal fun authErrorMessage(e: Throwable, fallback: String): String {
    val raw = e.message.orEmpty()
    val l = raw.lowercase()
    return when {
        "invalid login credentials" in l || "invalid_credentials" in l -> "E-mail ou senha incorretos"
        "email not confirmed" in l || "email_not_confirmed" in l -> "Confirme seu e-mail pelo link que enviamos antes de entrar"
        "already registered" in l || "already been registered" in l || "user_already_exists" in l ->
            "Este e-mail já possui cadastro. Faça login."
        "rate limit" in l || "too many" in l || "over_email_send_rate_limit" in l ->
            "Muitas tentativas. Aguarde um pouco e tente novamente."
        "weak_password" in l || "password should be" in l -> "Senha fraca: use pelo menos $MIN_PASSWORD_LENGTH caracteres"
        e is IOException || "unable to resolve host" in l || "timeout" in l || "failed to connect" in l ->
            "Sem conexão. Verifique sua internet e tente novamente."
        raw.isNotBlank() -> raw
        else -> fallback
    }
}
