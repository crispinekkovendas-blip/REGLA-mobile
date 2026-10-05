package br.com.imoveisregla.client.feature.apply

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.imoveisregla.core.designsystem.Regla
import br.com.imoveisregla.core.designsystem.SectionTitle
import br.com.imoveisregla.core.model.ClientDocument
import br.com.imoveisregla.core.model.DocumentKind
import br.com.imoveisregla.core.model.UploadFile
import br.com.imoveisregla.core.model.label
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Result of reading a picked file: either a ready upload or a user-facing error. */
sealed interface PickedFile {
    data class Ok(val file: UploadFile) : PickedFile
    data class Problem(val message: String) : PickedFile
}

/** Reads a SAF uri (≤ 10 MB, PDF or image). Must run off the main thread. */
fun readPickedFile(context: Context, uri: Uri): PickedFile {
    return try {
        val resolver = context.contentResolver
        var name = "documento"
        var size = -1L
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val ni = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val si = c.getColumnIndex(OpenableColumns.SIZE)
                if (ni >= 0 && !c.isNull(ni)) name = c.getString(ni) ?: name
                if (si >= 0 && !c.isNull(si)) size = c.getLong(si)
            }
        }
        val mime = resolver.getType(uri)
            ?: when {
                name.endsWith(".pdf", true) -> "application/pdf"
                name.endsWith(".png", true) -> "image/png"
                name.endsWith(".jpg", true) || name.endsWith(".jpeg", true) -> "image/jpeg"
                else -> null
            }
        if (size > MAX_UPLOAD_BYTES) return PickedFile.Problem("Arquivo maior que 10 MB")
        uploadProblem(mime, if (size < 0) 1 else size)?.let { return PickedFile.Problem(it) }
        val bytes = resolver.openInputStream(uri)?.use { input ->
            val out = java.io.ByteArrayOutputStream()
            val buf = ByteArray(64 * 1024)
            var total = 0L
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                total += n
                if (total > MAX_UPLOAD_BYTES) return PickedFile.Problem("Arquivo maior que 10 MB")
                out.write(buf, 0, n)
            }
            out.toByteArray()
        } ?: return PickedFile.Problem("Não foi possível ler o arquivo")
        uploadProblem(mime, bytes.size.toLong())?.let { return PickedFile.Problem(it) }
        PickedFile.Ok(UploadFile(name = name, mimeType = mime ?: "application/octet-stream", bytes = bytes))
    } catch (e: Exception) {
        PickedFile.Problem(e.message ?: "Não foi possível ler o arquivo")
    }
}

/** Wizard step 3 — "Documentos". */
@Composable
fun WizardDocumentsStep(
    documents: List<ClientDocument>,
    uploadedIds: List<Long>,
    uploadingKind: DocumentKind?,
    error: String?,
    onUpload: (DocumentKind, UploadFile) -> Unit,
    onError: (String) -> Unit,
    onRemove: (Long) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingKind by rememberSaveable { mutableStateOf<DocumentKind?>(null) }
    var showOptional by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val kind = pendingKind
        pendingKind = null
        if (uri != null && kind != null) {
            scope.launch {
                when (val picked = withContext(Dispatchers.IO) { readPickedFile(context, uri) }) {
                    is PickedFile.Ok -> onUpload(kind, picked.file)
                    is PickedFile.Problem -> onError(picked.message)
                }
            }
        }
    }
    val pick: (DocumentKind) -> Unit = { kind ->
        pendingKind = kind
        launcher.launch(arrayOf("application/pdf", "image/*"))
    }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("Documentos")
        Text(
            "Envie fotos ou PDFs (até 10 MB cada). Com os documentos em mãos, a análise é bem mais rápida.",
            style = MaterialTheme.typography.bodyMedium, color = Regla.Muted,
        )
        if (error != null) NoticeBox(error, color = Regla.Danger, background = Regla.DangerSoft)

        REQUIRED_DOCUMENTS.forEach { kind ->
            DocumentRow(
                kind = kind, required = true,
                files = documents.filter { it.kind == kind },
                uploadedIds = uploadedIds, uploading = uploadingKind == kind,
                enabled = uploadingKind == null,
                onPick = { pick(kind) }, onRemove = onRemove,
            )
        }

        val optionalWithFiles = OPTIONAL_DOCUMENTS.filter { k -> documents.any { it.kind == k } }
        val optionalShown = if (showOptional) OPTIONAL_DOCUMENTS else optionalWithFiles
        optionalShown.forEach { kind ->
            DocumentRow(
                kind = kind, required = false,
                files = documents.filter { it.kind == kind },
                uploadedIds = uploadedIds, uploading = uploadingKind == kind,
                enabled = uploadingKind == null,
                onPick = { pick(kind) }, onRemove = onRemove,
            )
        }
        if (!showOptional && optionalShown.size < OPTIONAL_DOCUMENTS.size) {
            TextButton(onClick = { showOptional = true }) { Text("Adicionar outros documentos") }
        }

        val missing = REQUIRED_DOCUMENTS.filter { k -> documents.none { it.kind == k } }
        if (missing.isNotEmpty()) {
            NoticeBox(
                title = "Faltam ${missing.size} documento(s) obrigatório(s)",
                text = "Você pode enviar depois em Perfil › Documentos, mas a análise só começa quando tudo estiver completo.",
            )
            TextButton(onClick = onSkip) { Text("Enviar depois") }
        }
    }
}

@Composable
private fun DocumentRow(
    kind: DocumentKind,
    required: Boolean,
    files: List<ClientDocument>,
    uploadedIds: List<Long>,
    uploading: Boolean,
    enabled: Boolean,
    onPick: () -> Unit,
    onRemove: (Long) -> Unit,
) {
    val done = files.isNotEmpty()
    Surface(
        shape = RoundedCornerShape(Regla.RadiusControl),
        color = Regla.Surface,
        border = BorderStroke(1.dp, if (done) Regla.Ok else Regla.Line),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (done) Icons.Outlined.CheckCircle else Icons.Outlined.Description,
                    contentDescription = null,
                    tint = if (done) Regla.Ok else Regla.Muted,
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(kind.label, style = MaterialTheme.typography.titleMedium)
                    Text(
                        when {
                            done -> if (files.size == 1) "1 arquivo enviado" else "${files.size} arquivos enviados"
                            required -> "Obrigatório"
                            else -> "Opcional"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (done) Regla.Ok else if (required) Regla.Coral else Regla.Muted,
                    )
                }
                if (uploading) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = Regla.Coral)
                } else {
                    OutlinedButton(onClick = onPick, enabled = enabled) {
                        Icon(Icons.Outlined.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (done) "Adicionar" else "Enviar")
                    }
                }
            }
            files.forEach { doc ->
                Row(Modifier.fillMaxWidth().padding(start = 34.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        doc.filename, style = MaterialTheme.typography.bodyMedium, color = Regla.Muted,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                    )
                    if (doc.id in uploadedIds) {
                        IconButton(onClick = { onRemove(doc.id) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Outlined.Close, contentDescription = "Remover ${doc.filename}", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}
