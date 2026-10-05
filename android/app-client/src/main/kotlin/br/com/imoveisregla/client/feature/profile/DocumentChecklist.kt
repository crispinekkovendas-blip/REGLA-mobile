package br.com.imoveisregla.client.feature.profile

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.imoveisregla.core.designsystem.Regla
import br.com.imoveisregla.core.designsystem.StatusPill
import br.com.imoveisregla.core.model.ClientDocument
import br.com.imoveisregla.core.model.DocumentKind
import br.com.imoveisregla.core.model.UploadFile
import br.com.imoveisregla.core.model.label
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Documents a proposta needs. Other kinds are optional. */
val REQUIRED_DOCUMENT_KINDS: List<DocumentKind> =
    listOf(DocumentKind.RG_CNH, DocumentKind.COMPROVANTE_RENDA, DocumentKind.COMPROVANTE_RESIDENCIA)

/** Required kinds first, then the optional ones. */
val CHECKLIST_ORDER: List<DocumentKind> =
    REQUIRED_DOCUMENT_KINDS + DocumentKind.entries.filter { it !in REQUIRED_DOCUMENT_KINDS }

const val MAX_DOCUMENT_BYTES: Long = 10L * 1024 * 1024

val DOCUMENT_MIME_TYPES: Array<String> = arrayOf("application/pdf", "image/*")

/** Required kind → whether at least one file was uploaded. */
fun requiredDocumentsStatus(documents: List<ClientDocument>): Map<DocumentKind, Boolean> =
    REQUIRED_DOCUMENT_KINDS.associateWith { kind -> documents.any { it.kind == kind } }

fun missingRequiredKinds(documents: List<ClientDocument>): List<DocumentKind> =
    requiredDocumentsStatus(documents).filterValues { !it }.keys.toList()

fun hasAllRequiredDocuments(documents: List<ClientDocument>): Boolean = missingRequiredKinds(documents).isEmpty()

class FileTooLargeException : IllegalArgumentException("O arquivo deve ter no máximo 10 MB")

/** Reads a picked document (name via DISPLAY_NAME, mime via getType). Throws [FileTooLargeException] above 10 MB. */
fun readUploadFile(context: Context, uri: Uri): UploadFile {
    val resolver = context.contentResolver
    var name: String? = null
    var size: Long? = null
    runCatching {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val ni = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val si = c.getColumnIndex(OpenableColumns.SIZE)
                if (ni >= 0 && !c.isNull(ni)) name = c.getString(ni)
                if (si >= 0 && !c.isNull(si)) size = c.getLong(si)
            }
        }
    }
    if ((size ?: 0L) > MAX_DOCUMENT_BYTES) throw FileTooLargeException()
    val mime = resolver.getType(uri) ?: "application/octet-stream"
    val bytes = resolver.openInputStream(uri)?.use { input ->
        val out = java.io.ByteArrayOutputStream()
        val buf = ByteArray(64 * 1024)
        var total = 0L
        while (true) {
            val n = input.read(buf)
            if (n < 0) break
            total += n
            if (total > MAX_DOCUMENT_BYTES) throw FileTooLargeException()
            out.write(buf, 0, n)
        }
        out.toByteArray()
    } ?: throw IllegalStateException("Não foi possível ler o arquivo")
    val fallbackName = uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "documento"
    return UploadFile(name = name?.takeIf { it.isNotBlank() } ?: fallbackName, mimeType = mime, bytes = bytes)
}

/**
 * Upload checklist used by "Meus documentos" and the proposal wizard.
 * "Enviar" opens the system document picker (PDF / images, max 10 MB) and calls [onUpload].
 * [onDelete] null hides the delete buttons; [onOpen] (optional) makes filenames clickable.
 */
@Composable
fun DocumentChecklist(
    documents: List<ClientDocument>,
    onUpload: (DocumentKind, UploadFile) -> Unit,
    onDelete: ((Long) -> Unit)?,
    uploadingKind: DocumentKind?,
    modifier: Modifier = Modifier,
    onOpen: ((ClientDocument) -> Unit)? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingKind by rememberSaveable { mutableStateOf<DocumentKind?>(null) }
    var pickError by rememberSaveable { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val kind = pendingKind
        pendingKind = null
        if (uri == null || kind == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { readUploadFile(context, uri) } }
            result.fold(
                onSuccess = { pickError = null; onUpload(kind, it) },
                onFailure = { pickError = it.message ?: "Não foi possível ler o arquivo" },
            )
        }
    }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        pickError?.let {
            Surface(color = Regla.DangerSoft, shape = RoundedCornerShape(Regla.RadiusControl)) {
                Text(
                    it, color = Regla.Danger, style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth().padding(12.dp).testTag("doc_error"),
                )
            }
        }
        CHECKLIST_ORDER.forEach { kind ->
            DocumentRow(
                kind = kind,
                required = kind in REQUIRED_DOCUMENT_KINDS,
                files = documents.filter { it.kind == kind },
                uploading = uploadingKind == kind,
                enabled = uploadingKind == null,
                onPick = {
                    pickError = null
                    pendingKind = kind
                    runCatching { launcher.launch(DOCUMENT_MIME_TYPES) }
                        .onFailure { pendingKind = null; pickError = "Nenhum aplicativo para escolher arquivos" }
                },
                onDelete = onDelete,
                onOpen = onOpen,
            )
        }
    }
}

@Composable
private fun DocumentRow(
    kind: DocumentKind,
    required: Boolean,
    files: List<ClientDocument>,
    uploading: Boolean,
    enabled: Boolean,
    onPick: () -> Unit,
    onDelete: ((Long) -> Unit)?,
    onOpen: ((ClientDocument) -> Unit)?,
) {
    val done = files.isNotEmpty()
    Surface(
        shape = RoundedCornerShape(Regla.RadiusCard),
        color = Regla.Surface,
        border = BorderStroke(1.dp, if (done) Regla.Ok.copy(alpha = 0.4f) else Regla.Line),
        modifier = Modifier.fillMaxWidth().testTag("doc_row_${kind.name}"),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when {
                        done -> Icons.Filled.CheckCircle
                        required -> Icons.Outlined.Schedule
                        else -> Icons.Outlined.RadioButtonUnchecked
                    },
                    contentDescription = if (done) "Enviado" else "Pendente",
                    tint = when {
                        done -> Regla.Ok
                        required -> Regla.Warn
                        else -> Regla.Muted
                    },
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(kind.label, style = MaterialTheme.typography.titleMedium, color = Regla.Ink)
                    Spacer(Modifier.padding(top = 2.dp))
                    when {
                        done -> StatusPill("Enviado", Regla.Ok, Regla.OkSoft)
                        required -> StatusPill("Obrigatório", Regla.Warn, Regla.WarnSoft)
                        else -> StatusPill("Opcional", Regla.Muted, Regla.NavySoft)
                    }
                }
                if (uploading) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = Regla.Coral)
                } else {
                    OutlinedButton(
                        onClick = onPick,
                        enabled = enabled,
                        shape = RoundedCornerShape(Regla.RadiusControl),
                        modifier = Modifier.testTag("doc_upload_${kind.name}"),
                    ) { Text("Enviar") }
                }
            }
            files.forEach { doc ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .let { m -> if (onOpen != null) m.clickable { onOpen(doc) } else m },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Description, contentDescription = null, tint = Regla.Muted, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            doc.filename, style = MaterialTheme.typography.bodyMedium, color = Regla.Ink,
                            fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        Text(formatBytes(doc.sizeBytes), style = MaterialTheme.typography.bodySmall, color = Regla.Muted)
                    }
                    if (onDelete != null) {
                        IconButton(onClick = { onDelete(doc.id) }, modifier = Modifier.testTag("doc_delete_${doc.id}")) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Excluir ${doc.filename}", tint = Regla.Muted)
                        }
                    }
                }
            }
        }
    }
}

fun formatBytes(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.1f MB", bytes / (1024.0 * 1024))
    bytes >= 1024 -> "${bytes / 1024} KB"
    else -> "$bytes B"
}
