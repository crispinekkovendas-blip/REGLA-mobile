package br.com.imoveisregla.core.data.supabase

import br.com.imoveisregla.core.data.AppContainer
import br.com.imoveisregla.core.data.fake.FakeBackend

/**
 * Supabase-backed container (supabase-kt: postgrest, auth, storage, realtime).
 * PLACEHOLDER: delegates to the demo backend until the real repositories land.
 */
class SupabaseContainer(
    @Suppress("unused") private val url: String,
    @Suppress("unused") private val anonKey: String,
) : AppContainer by FakeBackend(asRealtor = false) {
    override val isLive: Boolean = false
}
