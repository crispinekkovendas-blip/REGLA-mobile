package br.com.imoveisregla.core.model

// Form validation. Each validator returns field → error message (empty map = valid).
// Field keys match the ClientProfileInput / ApplicationInput / VisitInput property names.

private val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
private val DATE = Regex("^\\d{4}-\\d{2}-\\d{2}$")

fun validateProfile(p: ClientProfileInput, requireFinancials: Boolean = false): Map<String, String> = buildMap {
    if (p.fullName.trim().length < 3) put("fullName", "Informe seu nome completo")
    if (!EMAIL.matches(p.email.trim())) put("email", "E-mail inválido")
    if (digitsOnly(p.phone).length < 10) put("phone", "Telefone inválido")
    val cpf = p.cpf?.takeIf { it.isNotBlank() }
    if (cpf != null && !isValidCpf(cpf)) put("cpf", "CPF inválido")
    if (requireFinancials && cpf == null) put("cpf", "Informe seu CPF")
    p.birthDate?.takeIf { it.isNotBlank() }?.let { if (!DATE.matches(it)) put("birthDate", "Use AAAA-MM-DD") }
    if (p.monthlyIncome != null && p.monthlyIncome < 0) put("monthlyIncome", "Renda inválida")
    if (requireFinancials && (p.monthlyIncome == null || p.monthlyIncome <= 0)) put("monthlyIncome", "Informe sua renda mensal")
    if (requireFinancials && p.employmentType == null) put("employmentType", "Selecione o tipo de vínculo")
    if (p.residents !in 1..20) put("residents", "Entre 1 e 20 moradores")
}

fun validateApplication(a: ApplicationInput): Map<String, String> = buildMap {
    if (a.offeredPrice <= 0) put("offeredPrice", "Informe um valor")
    a.moveInDate?.takeIf { it.isNotBlank() }?.let { if (!DATE.matches(it)) put("moveInDate", "Use AAAA-MM-DD") }
    if ((a.message?.length ?: 0) > 2000) put("message", "Máximo de 2000 caracteres")
}

fun validateVisit(v: VisitInput): Map<String, String> = buildMap {
    if (v.visitorName.trim().length < 2) put("visitorName", "Informe seu nome")
    if (!EMAIL.matches(v.visitorEmail.trim())) put("visitorEmail", "E-mail inválido")
    if (v.startsAt.isBlank()) put("startsAt", "Escolha um horário")
}
