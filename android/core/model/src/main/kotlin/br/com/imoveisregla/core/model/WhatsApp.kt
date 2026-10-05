package br.com.imoveisregla.core.model

import java.net.URLEncoder

const val BROKER_WHATSAPP = "5511932210855" // +55 11 93221-0855

private fun enc(s: String) = URLEncoder.encode(s, "UTF-8").replace("+", "%20")

fun waUrl(text: String, phone: String = BROKER_WHATSAPP): String = "https://wa.me/$phone?text=${enc(text)}"

fun waInterest(title: String, ref: String): String =
    waUrl("Olá! Tenho interesse no imóvel \"$title\" (ref. $ref). Pode me passar mais informações?")

/** Realtor → client chat. BR numbers without country code get 55 prepended. */
fun waToClient(phone: String, text: String): String {
    var d = digitsOnly(phone)
    if (d.length <= 11) d = "55$d"
    return waUrl(text, d)
}
