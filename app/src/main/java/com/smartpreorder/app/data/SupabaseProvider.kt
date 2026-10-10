package com.smartpreorder.app.data

import com.smartpreorder.app.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json

import kotlin.time.Duration.Companion.seconds

/** One shared Supabase client for the whole app. Keys come from local.properties. */
val supabase: SupabaseClient = createSupabaseClient(
    supabaseUrl = BuildConfig.SUPABASE_URL,
    supabaseKey = BuildConfig.SUPABASE_ANON_KEY
) {
    requestTimeout = 30.seconds
    install(Auth)
    install(Postgrest)
    install(Realtime)
    defaultSerializer = KotlinXSerializer(Json { ignoreUnknownKeys = true })
}
