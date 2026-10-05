package br.com.imoveisregla.client.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.imoveisregla.client.LocalAppContainer
import br.com.imoveisregla.core.designsystem.ButtonKind
import br.com.imoveisregla.core.designsystem.Regla
import br.com.imoveisregla.core.designsystem.ReglaButton

@Composable
fun LoginScreen(onLoggedIn: () -> Unit, onSignup: () -> Unit, onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: LoginViewModel = viewModel { LoginViewModel(container.auth) }
    val state by vm.state.collectAsStateWithLifecycle()
    val focus = LocalFocusManager.current

    LaunchedEffect(state.signedIn) { if (state.signedIn) onLoggedIn() }

    AuthScaffold(onBack = onBack) {
        Text("Entrar", style = MaterialTheme.typography.headlineMedium, color = Regla.Ink)
        Spacer(Modifier.height(4.dp))
        Text(
            "Acesse para salvar favoritos, agendar visitas e acompanhar suas propostas.",
            style = MaterialTheme.typography.bodyMedium,
            color = Regla.Muted,
        )
        Spacer(Modifier.height(24.dp))
        AuthTextField(
            value = state.email,
            onValueChange = vm::onEmailChange,
            label = "E-mail",
            leadingIcon = Icons.Outlined.Mail,
            error = state.emailError,
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
            onImeAction = { focus.moveFocus(FocusDirection.Down) },
            enabled = !state.loading,
        )
        Spacer(Modifier.height(8.dp))
        PasswordField(
            value = state.password,
            onValueChange = vm::onPasswordChange,
            label = "Senha",
            visible = state.passwordVisible,
            onToggleVisible = vm::togglePasswordVisible,
            leadingIcon = Icons.Outlined.Lock,
            error = state.passwordError,
            imeAction = ImeAction.Done,
            onImeAction = { focus.clearFocus(); vm.submit() },
            enabled = !state.loading,
        )
        state.error?.let {
            Spacer(Modifier.height(12.dp))
            AuthErrorBox(it)
        }
        Spacer(Modifier.height(20.dp))
        ReglaButton(
            text = "Entrar",
            onClick = { focus.clearFocus(); vm.submit() },
            loading = state.loading,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Ainda não tem conta?", color = Regla.Muted, style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onSignup, enabled = !state.loading) {
                Text("Criar conta", color = Regla.Coral, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun SignupScreen(onSignedUp: () -> Unit, onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: SignupViewModel = viewModel { SignupViewModel(container.auth) }
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.signedUp) { if (state.signedUp) onSignedUp() }

    AuthScaffold(onBack = onBack) {
        if (state.needsConfirmation) {
            ConfirmEmailNotice(email = state.email.trim(), onBack = onBack)
        } else {
            SignupForm(state, vm, onBack)
        }
    }
}

@Composable
private fun SignupForm(state: SignupUiState, vm: SignupViewModel, onBack: () -> Unit) {
    val focus = LocalFocusManager.current
    Column(Modifier.fillMaxWidth()) {
        Text("Criar conta", style = MaterialTheme.typography.headlineMedium, color = Regla.Ink)
        Spacer(Modifier.height(4.dp))
        Text(
            "Leva menos de um minuto. Depois é só completar seu cadastro e enviar propostas pelo app.",
            style = MaterialTheme.typography.bodyMedium,
            color = Regla.Muted,
        )
        Spacer(Modifier.height(24.dp))
        AuthTextField(
            value = state.email,
            onValueChange = vm::onEmailChange,
            label = "E-mail",
            leadingIcon = Icons.Outlined.Mail,
            error = state.emailError,
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
            onImeAction = { focus.moveFocus(FocusDirection.Down) },
            enabled = !state.loading,
        )
        Spacer(Modifier.height(8.dp))
        PasswordField(
            value = state.password,
            onValueChange = vm::onPasswordChange,
            label = "Senha",
            visible = state.passwordVisible,
            onToggleVisible = vm::togglePasswordVisible,
            leadingIcon = Icons.Outlined.Lock,
            error = state.passwordError,
            imeAction = ImeAction.Next,
            onImeAction = { focus.moveFocus(FocusDirection.Down) },
            enabled = !state.loading,
        )
        if (state.passwordError == null) {
            Text(
                "Mínimo de $MIN_PASSWORD_LENGTH caracteres",
                style = MaterialTheme.typography.labelMedium,
                color = Regla.Muted,
                modifier = Modifier.padding(start = 16.dp, bottom = 4.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        PasswordField(
            value = state.confirm,
            onValueChange = vm::onConfirmChange,
            label = "Confirmar senha",
            visible = state.passwordVisible,
            onToggleVisible = vm::togglePasswordVisible,
            leadingIcon = Icons.Outlined.Lock,
            error = state.confirmError,
            imeAction = ImeAction.Done,
            onImeAction = { focus.clearFocus(); vm.submit() },
            enabled = !state.loading,
        )
        state.error?.let {
            Spacer(Modifier.height(12.dp))
            AuthErrorBox(it)
        }
        Spacer(Modifier.height(20.dp))
        ReglaButton(
            text = "Criar conta",
            onClick = { focus.clearFocus(); vm.submit() },
            kind = ButtonKind.Accent,
            loading = state.loading,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Ao criar sua conta, você concorda com os Termos de Uso e a Política de Privacidade da REGLA, " +
                "em conformidade com a LGPD.",
            style = MaterialTheme.typography.labelMedium,
            color = Regla.Muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Já tem conta?", color = Regla.Muted, style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onBack, enabled = !state.loading) {
                Text("Entrar", color = Regla.Coral, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}


@Composable
private fun ConfirmEmailNotice(email: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(8.dp))
        Icon(
            Icons.Outlined.MarkEmailRead,
            contentDescription = null,
            tint = Regla.Ok,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Regla.OkSoft)
                .padding(14.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text("Confirme seu e-mail", style = MaterialTheme.typography.headlineMedium, color = Regla.Ink)
        Spacer(Modifier.height(8.dp))
        Text(
            "Enviamos um link de confirmação para $email. Abra o link e depois faça login.",
            style = MaterialTheme.typography.bodyMedium,
            color = Regla.Muted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        ReglaButton("Voltar para o login", onClick = onBack, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun AuthScaffold(onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Regla.Surface)
            .imePadding()
            .verticalScroll(rememberScrollState()),
    ) {
        AuthHeader(onBack = onBack)
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 28.dp)) {
            content()
        }
    }
}
