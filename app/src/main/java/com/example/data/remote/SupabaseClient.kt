package com.example.data.remote

import io.github.jan_tennert.supabase.createSupabaseClient
import io.github.jan_tennert.supabase.postgrest.Postgrest
import io.github.jan_tennert.supabase.realtime.Realtime
import io.github.jan_tennert.supabase.storage.Storage

object SupabaseClient {
    // These should ideally be in BuildConfig or Strings
    private const val SUPABASE_URL = "https://tglplinxvrqsrxeicvpr.supabase.co"
    private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InRnbHBsaW54dnJxc3J4ZWljdnByIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODY4MTUxMDMsImV4cCI6MjEwMjM5MTEwM30.cVa5WJ3F4ie_-UCKFuZ8wIIqD5mRiiarJ0jgco7rhgM"

    val client = createSupabaseClient(SUPABASE_URL, SUPABASE_KEY) {
        install(Postgrest)
        install(Realtime)
        install(Storage)
    }
}
