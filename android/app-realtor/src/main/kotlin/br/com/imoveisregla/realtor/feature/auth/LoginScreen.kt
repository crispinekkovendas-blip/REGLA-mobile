package br.com.imoveisregla.realtor.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.imoveisregla.core.designsystem.Regla
import br.com.imoveisregla.core.designsystem.ReglaButton
import br.com.imoveisregla.realtor.LocalAppContainer

@Composable
fun LoginScreen(onLoggedIn: () -> Unit) {
    val container = LocalAppContainer.current
    val vm = viewModel { LoginViewModel(container.auth) }
    val state by vm.state.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .background(Regla.Surface)
            .imePadding()
            .verticalScroll(rememberScrollState()),
    ) {
        // Navy full-bleed header
        Column(
            Modifier
                .fillMaxWidth()
                .background(Regla.Navy)
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 40.dp),
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("REGLA", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.size(8.dp))
                Text(
                    "Corretor", color = Regla.Coral, fontSize = 22.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Leads, propostas e agenda em um só lugar.",
                color = Color.White.copy(alpha = 0.75f),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Column(
            Modifier.fillMaxWidth().padding(24.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Entrar", style = MaterialTheme.typography.headlineMedium, color = Regla.Ink)

            OutlinedTextField(
                value = state.email,
                onValueChange = vm::onEmail,
                label = { Text("E-mail") },
                singleLine = true,
                enabled = !state.loading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                shape = RoundedCornerShape(Regla.RadiusControl),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.password,
                onValueChange = vm::onPassword,
                label = { Text("Senha") },
                singleLine = true,
                enabled = !state.loading,
                visualTransformation = if (state.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { vm.submit(onLoggedIn) }),
                trailingIcon = {
                    IconButton(onClick = vm::togglePassword) {
                        Icon(
                            if (state.passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (state.passwordVisible) "Ocultar senha" else "Mostrar senha",
                        )
                    }
                },
                shape = RoundedCornerShape(Regla.RadiusControl),
                modifier = Modifier.fillMaxWidth(),
            )

            if (state.error != null) {
                Text(
                    state.error!!,
                    color = Regla.Danger,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Regla.RadiusControl))
                        .background(Regla.DangerSoft)
                        .padding(12.dp),
                )
            }

            ReglaButton(
                text = "Entrar",
                onClick = { vm.submit(onLoggedIn) },
                enabled = state.email.isNotBlank() && state.password.isNotEmpty(),
                loading = state.loading,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(4.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Regla.RadiusControl))
                    .background(Regla.NavySoft)
                    .padding(14.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = Regla.Navy, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(10.dp))
                Box(Modifier.weight(1f)) {
                    Text(
                        "Acesso fornecido pela REGLA. As contas de corretor são criadas pela administração — " +
                            "fale com a equipe se ainda não tem a sua.",
                        color = Regla.Muted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}
