package com.gdgc.meshroute.sync

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class FirestoreSyncManager {

    private val db: FirebaseFirestore = Firebase.firestore

    init {
        // Enable offline persistence
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        db.firestoreSettings = settings
    }

    /**
     * Saves a hazard report to Firestore. If the device is offline,
     * it will be cached locally and synced automatically when back online.
     */
    suspend fun uploadHazardReport(nodeName: String, hazardDescription: String) {
        val hazardData = hashMapOf(
            "nodeName" to nodeName,
            "description" to hazardDescription,
            "timestamp" to System.currentTimeMillis()
        )

        try {
            db.collection("hazards")
                .add(hazardData)
                .await()
        } catch (e: Exception) {
            // Handle error (though Firestore handles retry for offline cases automatically)
        }
    }

    /**
     * Listens for hazard updates from Firestore.
     * Firebase's offline persistence ensures this provides local data first if offline.
     */
    fun observeHazards(onUpdate: (List<Map<String, Any>>) -> Unit) {
        db.collection("hazards")
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                
                val list = snapshots?.documents?.map { it.data ?: emptyMap() } ?: emptyList()
                onUpdate(list)
            }
    }
}
