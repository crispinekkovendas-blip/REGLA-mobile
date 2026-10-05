package br.com.imoveisregla.core.data

import br.com.imoveisregla.core.data.fake.FakeBackend

/** Everything the UI needs. Built once per process in each app's Application/Activity. */
interface AppContainer {
    /** False when SUPABASE_URL / SUPABASE_ANON_KEY are missing → demo mode on fakes. */
    val isLive: Boolean
    val auth: AuthRepository
    val listings: ListingRepository
    val favorites: FavoriteRepository
    val profiles: ProfileRepository
    val applications: ApplicationRepository
    val documents: DocumentRepository
    val visits: VisitRepository
    val leads: LeadRepository
    val agenda: AgendaRepository
    val dashboard: DashboardRepository
}

object ReglaConfig {
    val supabaseUrl: String = BuildConfig.SUPABASE_URL
    val supabaseAnonKey: String = BuildConfig.SUPABASE_ANON_KEY
    val isConfigured: Boolean get() = supabaseUrl.isNotBlank() && supabaseAnonKey.isNotBlank()
}

/**
 * Factory used by both apps. Returns the Supabase-backed container when configured,
 * otherwise an in-memory demo container seeded with sample data.
 * [asRealtor] seeds the demo session as a realtor (realtor app) or a client (client app).
 */
object Containers {
    fun create(asRealtor: Boolean): AppContainer =
        if (ReglaConfig.isConfigured) supabaseContainer(ReglaConfig.supabaseUrl, ReglaConfig.supabaseAnonKey)
        else demo(asRealtor)

    fun demo(asRealtor: Boolean): AppContainer = FakeBackend(asRealtor = asRealtor)
}

/** Supabase implementation lives in the `supabase` package. */
internal fun supabaseContainer(url: String, anonKey: String): AppContainer =
    br.com.imoveisregla.core.data.supabase.SupabaseContainer(url, anonKey)
