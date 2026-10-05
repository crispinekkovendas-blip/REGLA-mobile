package br.com.imoveisregla.client.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import br.com.imoveisregla.core.designsystem.Regla
import br.com.imoveisregla.core.designsystem.SectionTitle
import br.com.imoveisregla.core.model.ClientProfile
import br.com.imoveisregla.core.model.ClientProfileInput
import br.com.imoveisregla.core.model.Currency
import br.com.imoveisregla.core.model.EmploymentType
import br.com.imoveisregla.core.model.digitsOnly
import br.com.imoveisregla.core.model.formatPrice
import br.com.imoveisregla.core.model.label
import br.com.imoveisregla.core.model.maskCpf
import br.com.imoveisregla.core.model.maskPhoneBR
import br.com.imoveisregla.core.model.validateProfile
import java.time.LocalDate

/**
 * Editable "meu cadastro" form values. Text fields hold what the user sees:
 * [phone] / [cpf] masked, [birthDate] as DD/MM/AAAA, [monthlyIncome] as digits only.
 * Reused by the proposal wizard (step 1).
 */
data class ProfileFormState(
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val cpf: String = "",
    val birthDate: String = "",
    val occupation: String = "",
    val employmentType: EmploymentType? = null,
    val monthlyIncome: String = "",
    val residents: Int = 1,
    val hasPets: Boolean = false,
) {
    fun toInput(): ClientProfileInput = ClientProfileInput(
        fullName = fullName.trim(),
        email = email.trim(),
        phone = phone.trim(),
        cpf = cpf.trim().takeIf { it.isNotEmpty() },
        birthDate = brDateToIso(birthDate),
        occupation = occupation.trim().takeIf { it.isNotEmpty() },
        employmentType = employmentType,
        monthlyIncome = digitsOnly(monthlyIncome).toLongOrNull(),
        residents = residents,
        hasPets = hasPets,
    )

    /** [validateProfile] on [toInput], with form-friendly messages. Empty map = valid. */
    fun validate(requireFinancials: Boolean = false): Map<String, String> =
        validateProfile(toInput(), requireFinancials).mapValues { (key, msg) ->
            if (key == "birthDate") "Data inválida (DD/MM/AAAA)" else msg
        }

    companion object {
        fun from(profile: ClientProfile?, sessionEmail: String?): ProfileFormState =
            if (profile == null) {
                ProfileFormState(email = sessionEmail.orEmpty())
            } else {
                ProfileFormState(
                    fullName = profile.fullName,
                    email = profile.email.ifBlank { sessionEmail.orEmpty() },
                    phone = maskPhoneBR(profile.phone),
                    cpf = profile.cpf?.let { maskCpf(it) }.orEmpty(),
                    birthDate = isoDateToBr(profile.birthDate),
                    occupation = profile.occupation.orEmpty(),
                    employmentType = profile.employmentType,
                    monthlyIncome = profile.monthlyIncome?.takeIf { it > 0 }?.toString().orEmpty(),
                    residents = profile.residents.coerceIn(1, 20),
                    hasPets = profile.hasPets,
                )
            }
    }
}

/** Progressive DD/MM/AAAA mask: "18041992" → "18/04/1992". */
fun maskDateBR(input: String): String {
    val d = digitsOnly(input).take(8)
    val sb = StringBuilder()
    d.forEachIndexed { i, c ->
        if (i == 2 || i == 4) sb.append('/')
        sb.append(c)
    }
    return sb.toString()
}

/** "18/04/1992" → "1992-04-18". Blank → null; incomplete/impossible dates are returned as typed (fails validation). */
fun brDateToIso(br: String): String? {
    val t = br.trim()
    if (t.isEmpty()) return null
    val d = digitsOnly(t)
    if (d.length != 8) return t
    val day = d.substring(0, 2).toInt()
    val month = d.substring(2, 4).toInt()
    val year = d.substring(4, 8).toInt()
    return runCatching { LocalDate.of(year, month, day).toString() }.getOrDefault(t)
}

/** "1992-04-18" → "18/04/1992". */
fun isoDateToBr(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    val p = iso.take(10).split("-")
    if (p.size != 3) return ""
    return "${p[2]}/${p[1]}/${p[0]}"
}

/** Digits → "R$ 18.000" for display; empty stays empty. */
fun formatIncomeInput(digits: String): String =
    digitsOnly(digits).toLongOrNull()?.let { formatPrice(it, Currency.BRL) }.orEmpty()

/** Fields counted by "Cadastro X% completo". */
fun profileCompletion(profile: ClientProfile?): Int {
    if (profile == null) return 0
    val filled = listOf(
        profile.fullName.isNotBlank(),
        profile.email.isNotBlank(),
        profile.phone.isNotBlank(),
        !profile.cpf.isNullOrBlank(),
        !profile.birthDate.isNullOrBlank(),
        !profile.occupation.isNullOrBlank(),
        profile.employmentType != null,
        (profile.monthlyIncome ?: 0) > 0,
    ).count { it }
    return filled * 100 / 8
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileForm(
    state: ProfileFormState,
    onChange: (ProfileFormState) -> Unit,
    errors: Map<String, String>,
    requireFinancials: Boolean,
    modifier: Modifier = Modifier,
) {
    val req = if (requireFinancials) " *" else ""
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("Dados pessoais")
        FormField(
            value = state.fullName, label = "Nome completo", error = errors["fullName"], tag = "fullName",
            onValueChange = { onChange(state.copy(fullName = it)) },
        )
        FormField(
            value = state.email, label = "E-mail", error = errors["email"], tag = "email",
            keyboardType = KeyboardType.Email,
            onValueChange = { onChange(state.copy(email = it.trim())) },
        )
        FormField(
            value = state.phone, label = "Celular", error = errors["phone"], tag = "phone",
            keyboardType = KeyboardType.Phone, format = ::maskPhoneBR,
            onValueChange = { onChange(state.copy(phone = it)) },
        )
        FormField(
            value = state.cpf, label = "CPF$req", error = errors["cpf"], tag = "cpf",
            keyboardType = KeyboardType.Number, format = ::maskCpf,
            onValueChange = { onChange(state.copy(cpf = it)) },
        )
        FormField(
            value = state.birthDate, label = "Data de nascimento (DD/MM/AAAA)", error = errors["birthDate"],
            tag = "birthDate", keyboardType = KeyboardType.Number, format = ::maskDateBR,
            onValueChange = { onChange(state.copy(birthDate = it)) },
        )

        Spacer(Modifier.padding(top = 4.dp))
        SectionTitle("Trabalho e renda")
        FormField(
            value = state.occupation, label = "Profissão", error = errors["occupation"], tag = "occupation",
            onValueChange = { onChange(state.copy(occupation = it)) },
        )
        Text("Vínculo$req", style = MaterialTheme.typography.labelLarge, color = Regla.Ink)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EmploymentType.entries.forEach { type ->
                FilterChip(
                    selected = state.employmentType == type,
                    onClick = {
                        onChange(state.copy(employmentType = if (state.employmentType == type) null else type))
                    },
                    label = { Text(type.label) },
                )
            }
        }
        errors["employmentType"]?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        FormField(
            value = formatIncomeInput(state.monthlyIncome), label = "Renda mensal$req", error = errors["monthlyIncome"],
            tag = "monthlyIncome", keyboardType = KeyboardType.Number, format = { formatIncomeInput(digitsOnly(it).take(9)) },
            onValueChange = { onChange(state.copy(monthlyIncome = digitsOnly(it).take(9))) },
        )

        Spacer(Modifier.padding(top = 4.dp))
        SectionTitle("Moradia")
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Moradores", style = MaterialTheme.typography.bodyLarge)
                Text("Incluindo você", style = MaterialTheme.typography.bodySmall, color = Regla.Muted)
            }
            FilledTonalIconButton(
                onClick = { onChange(state.copy(residents = (state.residents - 1).coerceAtLeast(1))) },
                enabled = state.residents > 1,
                modifier = Modifier.testTag("residents_minus"),
            ) { Icon(Icons.Filled.Remove, contentDescription = "Menos moradores") }
            Text(
                state.residents.toString(),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.width(40.dp).testTag("residents_value"),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            FilledTonalIconButton(
                onClick = { onChange(state.copy(residents = (state.residents + 1).coerceAtMost(20))) },
                enabled = state.residents < 20,
                modifier = Modifier.testTag("residents_plus"),
            ) { Icon(Icons.Filled.Add, contentDescription = "Mais moradores") }
        }
        errors["residents"]?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Tenho animais de estimação", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Switch(
                checked = state.hasPets,
                onCheckedChange = { onChange(state.copy(hasPets = it)) },
                modifier = Modifier.testTag("hasPets"),
            )
        }

        Surface(color = Regla.InfoSoft, shape = RoundedCornerShape(Regla.RadiusControl)) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Lock, contentDescription = null, tint = Regla.Info)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Seus dados estão protegidos", fontWeight = FontWeight.SemiBold, color = Regla.Ink)
                    Text(
                        "Usamos seus dados apenas para análise da proposta, conforme a LGPD.",
                        style = MaterialTheme.typography.bodySmall, color = Regla.Muted,
                    )
                }
            }
        }
    }
}

/**
 * Outlined field that applies [format] (mask) on every keystroke and keeps the cursor at the end
 * when the mask changes the text.
 */
@Composable
private fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    tag: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    format: (String) -> String = { it },
) {
    var tfv by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    val shown = if (tfv.text == value) tfv else TextFieldValue(value, TextRange(value.length))
    OutlinedTextField(
        value = shown,
        onValueChange = { new ->
            val formatted = format(new.text)
            tfv = if (formatted == new.text) new else TextFieldValue(formatted, TextRange(formatted.length))
            if (formatted != value) onValueChange(formatted)
        },
        label = { Text(label) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        shape = RoundedCornerShape(Regla.RadiusControl),
        modifier = Modifier.fillMaxWidth().testTag("profile_$tag"),
    )
}
