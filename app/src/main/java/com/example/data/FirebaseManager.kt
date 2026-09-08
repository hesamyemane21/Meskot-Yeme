package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseManager {
    private const val TAG = "FirebaseManager"
    private var isInitialized = false

    var auth: FirebaseAuth? = null
        private set
    var firestore: FirebaseFirestore? = null
        private set

    fun initialize(context: Context) {
        if (isInitialized) return
        try {
            val existingApps = FirebaseApp.getApps(context)
            val app = if (existingApps.isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApiKey("AIzaSyD9j6Emef7z1sxtGuqgEePwIJetPo4ZhQQ")
                    .setApplicationId("1:1083041536701:web:4654f20e5ae08a2d0435d4")
                    .setProjectId("meskot-b1a69")
                    .setStorageBucket("meskot-b1a69.firebasestorage.app")
                    .build()
                FirebaseApp.initializeApp(context, options)
            } else {
                existingApps.first()
            }

            auth = FirebaseAuth.getInstance(app)
            firestore = FirebaseFirestore.getInstance(app)
            isInitialized = true
            Log.d(TAG, "Firebase initialized successfully with project meskot-b1a69")
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization fallback (offline/cached mode): ${e.message}")
        }
    }

    fun isConfigured(): Boolean = isInitialized && auth != null && firestore != null
}
