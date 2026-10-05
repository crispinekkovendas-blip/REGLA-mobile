package br.com.imoveisregla.realtor.feature.leads

import androidx.compose.runtime.Composable
import br.com.imoveisregla.realtor.ui.Stub

@Composable
fun LeadsScreen(onOpenLead: (Long) -> Unit) = Stub("LeadsScreen")

@Composable
fun LeadDetailScreen(leadId: Long, onBack: () -> Unit, onOpenListing: (Long) -> Unit) = Stub("LeadDetailScreen")
