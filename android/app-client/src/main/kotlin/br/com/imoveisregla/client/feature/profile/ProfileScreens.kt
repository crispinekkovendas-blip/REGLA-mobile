package br.com.imoveisregla.client.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.imoveisregla.client.LocalAppContainer
import br.com.imoveisregla.core.data.SessionState
import br.com.imoveisregla.core.designsystem.ButtonKind
import br.com.imoveisregla.core.designsystem.ErrorState
import br.com.imoveisregla.core.designsystem.LoadingState
import br.com.imoveisregla.core.designsystem.Regla
import br.com.imoveisregla.core.designsystem.ReglaButton
import br.com.imoveisregla.core.model.waUrl

// ─── Profile tab ──────────────────────────────────────────────────────

@Composable
fun ProfileScreen(onEditProfile: () -> Unit, onDocuments: () -> Unit, onLogin: () -> Unit) {
    val container = LocalAppContainer.current
    val vm = viewModel { ProfileViewModel(container) }
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.refresh() }

    Column(Modifier.fillMaxSize().background(Regla.NavySoft)) {
        when (val s = state.session) {
            SessionState.Loading -> LoadingState()
            SessionState.SignedOut -> SignedOutProfile(onLogin)
            is SessionState.SignedIn -> when {
                state.loading -> LoadingState()
                state.error != null && state.profile == null -> ErrorState(state.error!!, onRetry = vm::refresh)
                else -> SignedInProfile(
                    state = state,
                    fallbackEmail = s.session.email,
                    onEditProfile = onEditProfile,
                    onDocuments = onDocuments,
                    onSignOut = vm::signOut,
                )
            }
        }
    }
}

@Composable
private fun SignedOutProfile(onLogin: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(96.dp).clip(CircleShape).background(Regla.CoralSoft),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Outlined.Person, contentDescription = null, tint = Regla.Coral, modifier = Modifier.size(48.dp)) }
        Spacer(Modifier.height(20.dp))
        Text(
            "Seu perfil na REGLA", style = MaterialTheme.typography.headlineMedium, color = Regla.Ink,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Entre para salvar favoritos, agendar visitas, enviar documentos e acompanhar suas propostas.",
            color = Regla.Muted, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        ReglaButton("Entrar ou criar conta", onLogin, kind = ButtonKind.Accent, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(24.dp))
        AppVersionText()
    }
}

@Composable
private fun SignedInProfile(
    state: ProfileUiState,
    fallbackEmail: String,
    onEditProfile: () -> Unit,
    onDocuments: () -> Unit,
    onSignOut: () -> Unit,
) {
    val uri = LocalUriHandler.current
    var confirmSignOut by rememberSaveable { mutableStateOf(false) }
    val profile = state.profile
    val name = profile?.fullName?.takeIf { it.isNotBlank() }
    val email = profile?.email?.takeIf { it.isNotBlank() } ?: fallbackEmail
    val completion = state.completion

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Regla.Gutter),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Surface(shape = RoundedCornerShape(Regla.RadiusCard), color = Regla.Surface, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(64.dp).clip(CircleShape).background(Regla.Navy),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            initials(name ?: email), color = Color.White,
                            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            name ?: "Complete seu cadastro", style = MaterialTheme.typography.titleLarge, color = Regla.Ink,
                            modifier = Modifier.testTag("profile_name"),
                        )
                        Text(email, color = Regla.Muted, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Cadastro $completion% completo", style = MaterialTheme.typography.labelLarge, color = Regla.Ink,
                        modifier = Modifier.weight(1f),
                    )
                    if (completion < 100) {
                        TextButton(onClick = onEditProfile) { Text("Completar", color = Regla.Coral) }
                    }
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { completion / 100f },
                    color = if (completion >= 100) Regla.Ok else Regla.Coral,
                    trackColor = Regla.Line,
                    strokeCap = StrokeCap.Round,
                    modifier = Modifier.fillMaxWidth().height(8.dp).testTag("profile_completion"),
                )
                if (completion < 100) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Um cadastro completo agiliza a análise das suas propostas.",
                        style = MaterialTheme.typography.bodySmall, color = Regla.Muted,
                    )
                }
            }
        }

        Surface(shape = RoundedCornerShape(Regla.RadiusCard), color = Regla.Surface, modifier = Modifier.fillMaxWidth()) {
            Column {
                MenuRow(Icons.Outlined.Badge, "Meu cadastro", "Dados pessoais, renda e moradia", onClick = onEditProfile)
                HorizontalDivider(color = Regla.Line)
                MenuRow(
                    Icons.Outlined.Folder, "Meus documentos",
                    when (state.documentCount) {
                        0 -> "Nenhum documento enviado"
                        1 -> "1 documento"
                        else -> "${state.documentCount} documentos"
                    },
                    onClick = onDocuments,
                )
                HorizontalDivider(color = Regla.Line)
                MenuRow(Icons.AutoMirrored.Outlined.Chat, "Falar com a REGLA", "Atendimento pelo WhatsApp") {
                    runCatching { uri.openUri(waUrl("Olá! Preciso de ajuda com meu cadastro no app da REGLA.")) }
                }
                HorizontalDivider(color = Regla.Line)
                MenuRow(Icons.AutoMirrored.Outlined.Logout, "Sair", null, tint = Regla.Danger) { confirmSignOut = true }
            }
        }

        AppVersionText(Modifier.fillMaxWidth())
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text("Sair da conta?") },
            text = { Text("Você precisará entrar novamente para ver suas propostas e documentos.") },
            confirmButton = {
                TextButton(onClick = { confirmSignOut = false; onSignOut() }) { Text("Sair", color = Regla.Danger) }
            },
            dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun MenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    tint: Color = Regla.Navy,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = if (tint == Regla.Danger) Regla.Danger else Regla.Ink)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Regla.Muted)
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = Regla.Muted)
    }
}

@Composable
private fun AppVersionText(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val version = remember {
        runCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "—"
    }
    Text(
        "REGLA Imóveis · versão $version", style = MaterialTheme.typography.bodySmall, color = Regla.Muted,
        textAlign = TextAlign.Center, modifier = modifier,
    )
}

internal fun initials(nameOrEmail: String): String {
    val base = nameOrEmail.substringBefore('@').trim()
    val parts = base.split(Regex("[\\s._-]+")).filter { it.isNotEmpty() }
    val letters = when {
        parts.size >= 2 -> "${parts.first().first()}${parts.last().first()}"
        parts.size == 1 -> parts.first().take(2)
        else -> "?"
    }
    return letters.uppercase()
}

// ─── Edit profile ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val vm = viewModel { EditProfileViewModel(container) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            vm.consumeMessage()
        }
    }

    Scaffold(
        topBar = { BackTopBar("Meu cadastro", onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = Regla.Surface,
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                state.loading -> LoadingState()
                state.loadError != null -> ErrorState(state.loadError!!, onRetry = vm::load)
                else -> Column(Modifier.fillMaxSize()) {
                    Column(
                        Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(Regla.Gutter),
                    ) {
                        Text(
                            "Mantenha seus dados atualizados para enviar propostas mais rápido.",
                            color = Regla.Muted, style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(Modifier.height(16.dp))
                        ProfileForm(
                            state = state.form,
                            onChange = vm::onChange,
                            errors = state.errors,
                            requireFinancials = false,
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                    HorizontalDivider(color = Regla.Line)
                    ReglaButton(
                        "Salvar cadastro", vm::save, loading = state.saving,
                        modifier = Modifier.fillMaxWidth().padding(Regla.Gutter).testTag("profile_save"),
                    )
                }
            }
        }
    }
}

// ─── Documents ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val vm = viewModel { DocumentsViewModel(container) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val uri = LocalUriHandler.current
    var confirmDelete by rememberSaveable { mutableStateOf<Long?>(null) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            vm.consumeMessage()
        }
    }

    Scaffold(
        topBar = { BackTopBar("Meus documentos", onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = Regla.NavySoft,
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                state.loading -> LoadingState()
                state.loadError != null -> ErrorState(state.loadError!!, onRetry = vm::load)
                else -> Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Regla.Gutter),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    RequiredSummary(done = state.requiredDone, total = REQUIRED_DOCUMENT_KINDS.size)
                    DocumentChecklist(
                        documents = state.documents,
                        onUpload = vm::upload,
                        onDelete = { confirmDelete = it },
                        uploadingKind = state.uploadingKind,
                        onOpen = { doc -> vm.open(doc) { url -> runCatching { uri.openUri(url) } } },
                    )
                    Text(
                        "Formatos aceitos: PDF, JPG ou PNG de até 10 MB. Seus documentos ficam visíveis apenas para você e para a equipe da REGLA.",
                        style = MaterialTheme.typography.bodySmall, color = Regla.Muted,
                    )
                }
            }
        }
    }

    confirmDelete?.let { id ->
        val doc = state.documents.firstOrNull { it.id == id }
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Excluir documento?") },
            text = { Text(doc?.filename?.let { "\"$it\" será removido." } ?: "O documento será removido.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = null; vm.delete(id) }) { Text("Excluir", color = Regla.Danger) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun RequiredSummary(done: Int, total: Int) {
    val complete = done >= total
    Surface(
        shape = RoundedCornerShape(Regla.RadiusCard),
        color = if (complete) Regla.OkSoft else Regla.WarnSoft,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                if (complete) "Documentação completa" else "$done de $total documentos obrigatórios enviados",
                style = MaterialTheme.typography.titleMedium, color = Regla.Ink,
                modifier = Modifier.testTag("docs_summary"),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (complete) "Você já pode enviar propostas sem pendências."
                else "Envie RG ou CNH, comprovante de renda e comprovante de residência para agilizar sua proposta.",
                style = MaterialTheme.typography.bodySmall, color = Regla.Muted,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackTopBar(title: String, onBack: () -> Unit) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar") }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Regla.Surface),
    )
}
