package br.com.imoveisregla.client.feature.apply

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.com.imoveisregla.core.designsystem.Regla
import br.com.imoveisregla.core.designsystem.ReglaTextField
import br.com.imoveisregla.core.designsystem.SectionTitle
import br.com.imoveisregla.core.model.Currency
import br.com.imoveisregla.core.model.EmploymentType
import br.com.imoveisregla.core.model.formatPrice
import br.com.imoveisregla.core.model.label
import br.com.imoveisregla.core.model.maskCpf
import br.com.imoveisregla.core.model.maskPhoneBR

/** Wizard step 1 — "Cadastro". Self-contained (the shared ProfileForm may replace it later). */
@Composable
fun WizardProfileStep(
    draft: WizardProfileDraft,
    errors: Map<String, String>,
    onChange: ((WizardProfileDraft) -> WizardProfileDraft) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("Seus dados")
        Text(
            "Usamos essas informações para analisar sua proposta. Elas ficam salvas no seu cadastro.",
            style = MaterialTheme.typography.bodyMedium, color = Regla.Muted,
        )
        ReglaTextField(
            value = draft.fullName, onValueChange = { v -> onChange { it.copy(fullName = v) } },
            label = "Nome completo", error = errors["fullName"],
        )
        ReglaTextField(
            value = draft.email, onValueChange = { v -> onChange { it.copy(email = v) } },
            label = "E-mail", error = errors["email"], keyboardType = KeyboardType.Email,
        )
        MaskedDigitsField(
            digits = draft.phoneDigits, onDigitsChange = { v -> onChange { it.copy(phoneDigits = v) } },
            label = "Celular", format = ::maskPhoneBR, maxDigits = 11, error = errors["phone"],
        )
        MaskedDigitsField(
            digits = draft.cpfDigits, onDigitsChange = { v -> onChange { it.copy(cpfDigits = v) } },
            label = "CPF", format = ::maskCpf, maxDigits = 11, error = errors["cpf"],
        )

        Spacer(Modifier.width(4.dp))
        SectionTitle("Renda")
        FieldLabel("Vínculo")
        ChoiceChips(
            options = EmploymentType.entries,
            selected = draft.employmentType,
            label = { it.label },
            onSelect = { v -> onChange { it.copy(employmentType = v) } },
        )
        FieldError(errors["employmentType"])
        MaskedDigitsField(
            digits = draft.incomeDigits, onDigitsChange = { v -> onChange { it.copy(incomeDigits = v) } },
            label = "Renda mensal", format = { formatPrice(it.toLong(), Currency.BRL) }, maxDigits = 9,
            error = errors["monthlyIncome"], supporting = "Some a renda de todos que vão morar no imóvel",
            stripLeadingZeros = true,
        )

        SectionTitle("Moradia")
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Moradores", style = MaterialTheme.typography.bodyLarge)
                Text("Incluindo você", style = MaterialTheme.typography.labelMedium, color = Regla.Muted)
            }
            OutlinedIconButton(
                onClick = { onChange { it.copy(residents = (it.residents - 1).coerceAtLeast(1)) } },
                enabled = draft.residents > 1,
            ) { Icon(Icons.Outlined.Remove, contentDescription = "Menos moradores") }
            Text(
                "${draft.residents}", style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.width(36.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            OutlinedIconButton(
                onClick = { onChange { it.copy(residents = (it.residents + 1).coerceAtMost(20)) } },
                enabled = draft.residents < 20,
            ) { Icon(Icons.Outlined.Add, contentDescription = "Mais moradores") }
        }
        FieldError(errors["residents"])
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Tenho pets", style = MaterialTheme.typography.bodyLarge)
                Text("Cães, gatos ou outros animais", style = MaterialTheme.typography.labelMedium, color = Regla.Muted)
            }
            Switch(
                checked = draft.hasPets,
                onCheckedChange = { v -> onChange { it.copy(hasPets = v) } },
                colors = SwitchDefaults.colors(checkedTrackColor = Regla.Navy),
            )
        }
    }
}
