package br.com.imoveisregla.core.data.supabase

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonPrimitive

/**
 * JSON config shared by the Supabase client (supabase-kt `defaultSerializer`) and the tests.
 * - ignoreUnknownKeys: PostgREST `*` returns columns the models don't map (palette, shape, …)
 * - coerceInputValues: `null` / unknown enum values fall back to the property default
 * - encodeDefaults + explicitNulls: insert/update DTOs always send every column they declare
 */
val ReglaJson: Json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    encodeDefaults = true
    explicitNulls = true
}

/** Wire value of a @Serializable enum (its @SerialName), e.g. `ListingStatus.LIVE` → `"live"`. */
internal inline fun <reified T> wire(value: T): String = ReglaJson.encodeToJsonElement(value).jsonPrimitive.content
