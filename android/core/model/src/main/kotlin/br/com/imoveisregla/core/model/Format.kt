package br.com.imoveisregla.core.model

import java.text.NumberFormat
import java.util.Locale

private val PT_BR: Locale = Locale.forLanguageTag("pt-BR")

/** formatPrice(1850000, BRL) → "R$ 1.850.000" (no decimals, normal spaces). */
fun formatPrice(amount: Long, currency: Currency, locale: Locale = PT_BR): String {
    val nf = NumberFormat.getCurrencyInstance(locale)
    nf.currency = java.util.Currency.getInstance(currency.name)
    nf.maximumFractionDigits = 0
    nf.minimumFractionDigits = 0
    return nf.format(amount).replace(' ', ' ').replace(' ', ' ')
}

fun formatArea(m2: Int): String = "${NumberFormat.getIntegerInstance(PT_BR).format(m2)} m²"

/** Listing reference code shown to clients and brokers, e.g. "RG-0042". */
fun listingRef(id: Long): String = "RG-" + id.toString().padStart(4, '0')

fun digitsOnly(s: String): String = s.filter { it.isDigit() }

/** Validate a Brazilian CPF including both check digits. */
fun isValidCpf(input: String): Boolean {
    val cpf = digitsOnly(input)
    if (cpf.length != 11 || cpf.all { it == cpf[0] }) return false
    fun digit(len: Int): Int {
        var sum = 0
        for (i in 0 until len) sum += (cpf[i] - '0') * (len + 1 - i)
        val r = (sum * 10) % 11
        return if (r == 10) 0 else r
    }
    return digit(9) == cpf[9] - '0' && digit(10) == cpf[10] - '0'
}

/** "52998224725" → "529.982.247-25" (partial input is masked progressively). */
fun maskCpf(input: String): String {
    val d = digitsOnly(input).take(11)
    val sb = StringBuilder()
    d.forEachIndexed { i, c ->
        if (i == 3 || i == 6) sb.append('.')
        if (i == 9) sb.append('-')
        sb.append(c)
    }
    return sb.toString()
}

/** "11932210855" → "(11) 93221-0855"; "1132210855" → "(11) 3221-0855". */
fun maskPhoneBR(input: String): String {
    val d = digitsOnly(input).take(11)
    if (d.isEmpty()) return ""
    if (d.length <= 2) return "(" + d
    if (d.length <= 6) return "(${d.take(2)}) ${d.drop(2)}"
    val split = d.length - 4
    return "(${d.take(2)}) ${d.substring(2, split)}-${d.substring(split)}"
}

enum class Affordability { OK, TIGHT, OVER, UNKNOWN }

/** QuintoAndar-style rule: rent should be ≤ 30% of household income (≤ 40% is "tight"). */
fun affordability(monthlyRent: Long, monthlyIncome: Long?): Affordability {
    if (monthlyIncome == null || monthlyIncome <= 0) return Affordability.UNKNOWN
    val ratio = monthlyRent.toDouble() / monthlyIncome
    return when {
        ratio <= 0.30 -> Affordability.OK
        ratio <= 0.40 -> Affordability.TIGHT
        else -> Affordability.OVER
    }
}

/** Storage path for client documents: `${userId}/${kind}/${now}-${safeName}`. */
fun documentPath(userId: String, kind: DocumentKind, filename: String, now: Long = System.currentTimeMillis()): String {
    val normalized = java.text.Normalizer.normalize(filename, java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .replace(Regex("[^a-zA-Z0-9._-]"), "_")
    val kindSlug = kind.name.lowercase()
    return "$userId/$kindSlug/$now-$normalized"
}
