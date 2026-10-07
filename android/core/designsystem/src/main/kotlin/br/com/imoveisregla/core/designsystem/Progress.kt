package br.com.imoveisregla.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.imoveisregla.core.model.ApplicationStatus
import br.com.imoveisregla.core.model.label

/** White top bar with optional back arrow; title in bold navy. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReglaTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Regla.Surface,
            scrolledContainerColor = Regla.Surface,
            titleContentColor = Regla.Navy,
            navigationIconContentColor = Regla.Navy,
            actionIconContentColor = Regla.Navy,
        ),
        modifier = modifier,
    )
}

/**
 * Horizontal wizard progress: numbered circles joined by lines, labels underneath.
 * Steps before [current] show a check; [current] is coral.
 */
@Composable
fun StepIndicator(steps: List<String>, current: Int, modifier: Modifier = Modifier) {
    if (steps.isEmpty()) return
    val cur = current.coerceIn(0, steps.lastIndex)
    Column(
        modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Etapa ${cur + 1} de ${steps.size}: ${steps[cur]}" },
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            steps.forEachIndexed { i, _ ->
                StepDot(index = i, done = i < cur, active = i == cur)
                if (i < steps.lastIndex) {
                    Box(
                        Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp)
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(if (i < cur) Regla.Navy else Regla.Line),
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            steps.forEachIndexed { i, s ->
                Text(
                    s,
                    style = MaterialTheme.typography.labelMedium,
                    color = when {
                        i == cur -> Regla.Ink
                        i < cur -> Regla.Muted
                        else -> Regla.Subtle
                    },
                    fontWeight = if (i == cur) FontWeight.Bold else FontWeight.Medium,
                    textAlign = when (i) {
                        0 -> TextAlign.Start
                        steps.lastIndex -> TextAlign.End
                        else -> TextAlign.Center
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun StepDot(index: Int, done: Boolean, active: Boolean) {
    val bg = when {
        done -> Regla.Navy
        active -> Regla.Coral
        else -> Color.White
    }
    Box(
        Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(bg)
            .then(if (!done && !active) Modifier.border(1.5.dp, Regla.Line, CircleShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (done) {
            Icon(Icons.Rounded.Check, contentDescription = "Etapa concluída", tint = Color.White, modifier = Modifier.size(16.dp))
        } else {
            Text(
                "${index + 1}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (active) Color.White else Regla.Subtle,
            )
        }
    }
}

/**
 * Vertical progress timeline (e.g. proposta: Enviada → Em análise → Aprovada).
 * Steps before [currentIndex] are done, [currentIndex] is highlighted with [currentColor];
 * pass [failed] = true to mark the current step as a negative outcome (red, ✕).
 */
@Composable
fun StatusTimeline(
    steps: List<String>,
    currentIndex: Int,
    modifier: Modifier = Modifier,
    failed: Boolean = false,
    currentColor: Color = Regla.Coral,
) {
    Column(modifier.fillMaxWidth()) {
        steps.forEachIndexed { i, s ->
            val done = i < currentIndex
            val active = i == currentIndex
            Row(Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val dotColor = when {
                        active && failed -> Regla.Danger
                        active -> currentColor
                        done -> Regla.Navy
                        else -> Regla.Line
                    }
                    Box(
                        Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(if (active) dotColor.copy(alpha = 0.18f) else Color.Transparent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier
                                .size(if (active) 12.dp else 14.dp)
                                .clip(CircleShape)
                                .background(dotColor),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (done) Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                            if (active && failed) Icon(Icons.Rounded.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(9.dp))
                        }
                    }
                    if (i < steps.lastIndex) {
                        Box(
                            Modifier
                                .width(2.dp)
                                .heightIn(min = 26.dp)
                                .height(26.dp)
                                .background(if (done) Regla.Navy else Regla.Line),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    s,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        active && failed -> Regla.DangerInk
                        active || done -> Regla.Ink
                        else -> Regla.Subtle
                    },
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
        }
    }
}

/**
 * Timeline steps + current index for a proposta (QuintoAndar-style lifecycle):
 * Enviada → Negociação → Aceita → Documentos → Aprovada (or Recusada / Cancelada).
 */
fun applicationTimeline(status: ApplicationStatus): Pair<List<String>, Int> {
    val docs = when (status) {
        ApplicationStatus.DOCS_REVIEW, ApplicationStatus.DOCS_REQUESTED -> status.label
        else -> "Documentos"
    }
    val final = when (status) {
        ApplicationStatus.APPROVED, ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN -> status.label
        else -> "Contrato"
    }
    val idx = when (status) {
        ApplicationStatus.SUBMITTED, ApplicationStatus.UNDER_REVIEW -> 0
        ApplicationStatus.NEGOTIATING -> 1
        ApplicationStatus.ACCEPTED -> 2
        ApplicationStatus.DOCS_REVIEW, ApplicationStatus.DOCS_REQUESTED -> 3
        ApplicationStatus.APPROVED, ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN -> 4
    }
    return listOf(ApplicationStatus.SUBMITTED.label, "Negociação", ApplicationStatus.ACCEPTED.label, docs, final) to idx
}

@Composable
fun ApplicationStatusTimeline(status: ApplicationStatus, modifier: Modifier = Modifier) {
    val (steps, idx) = applicationTimeline(status)
    StatusTimeline(
        steps = steps,
        currentIndex = idx,
        modifier = modifier,
        failed = status == ApplicationStatus.REJECTED || status == ApplicationStatus.WITHDRAWN,
        currentColor = if (status == ApplicationStatus.APPROVED) Regla.Ok else Regla.Coral,
    )
}
