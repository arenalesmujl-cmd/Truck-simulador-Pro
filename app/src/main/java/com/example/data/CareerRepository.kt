package com.example.data

import android.content.Context
import com.example.R
import com.example.model.CareerProfile
import com.example.model.DeliveryRecord
import com.example.model.GearboxMode
import com.example.model.GraphicsQuality
import com.example.model.SteeringMode
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CareerRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        }
        return uid
    }

    fun observeCareerProfile(userId: String): Flow<CareerProfile?> = flow {
        requireUserId()
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (!snapshot.exists()) null
                    else snapshot.toObject(
                        CareerProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeDeliveries(userId: String): Flow<List<DeliveryRecord>> = flow {
        requireUserId()
        val path = "users/$userId/deliveries"
        emitAll(
            db.collection("users").document(userId).collection("deliveries")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(30)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        DeliveryRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureInitialProfile(userId: String, displayName: String?): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Refusing cross-user profile creation" }
            val docRef = db.collection("users").document(uid)
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val safeName = displayName?.trim()?.take(40)?.ifBlank { "Capitan Ruta" } ?: "Capitan Ruta"
                val payload = mapOf(
                    "userId" to uid,
                    "driverName" to safeName,
                    "money" to 25000L,
                    "xp" to 0L,
                    "level" to 1L,
                    "selectedTruckId" to "titan_680",
                    "ownedTruckIds" to listOf("titan_680"),
                    "truckPaintHex" to "#FF6D00",
                    "engineLevel" to 1L,
                    "transmissionLevel" to 1L,
                    "suspensionLevel" to 1L,
                    "tankLevel" to 1L,
                    "unlockedRouteIds" to listOf("route_valley"),
                    "totalDistanceKm" to 0.0,
                    "completedMissionsCount" to 0L,
                    "graphicsQuality" to GraphicsQuality.HIGH.name,
                    "gearboxMode" to GearboxMode.AUTO.name,
                    "steeringMode" to SteeringMode.WHEEL.name,
                    "soundEnabled" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateGarageAndTruck(
        userId: String,
        money: Long,
        selectedTruckId: String,
        ownedTruckIds: List<String>,
        truckPaintHex: String,
        engineLevel: Long,
        transmissionLevel: Long,
        suspensionLevel: Long,
        tankLevel: Long
    ): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Refusing cross-user garage update" }
            require(money >= 0L) { "Insufficient funds" }
            require(ownedTruckIds.isNotEmpty() && ownedTruckIds.size <= 30)
            require(truckPaintHex.matches(Regex("^#[0-9A-Fa-f]{6}$")))
            db.collection("users").document(uid).update(
                mapOf(
                    "money" to money,
                    "selectedTruckId" to selectedTruckId,
                    "ownedTruckIds" to ownedTruckIds,
                    "truckPaintHex" to truckPaintHex,
                    "engineLevel" to engineLevel.coerceIn(1L, 5L),
                    "transmissionLevel" to transmissionLevel.coerceIn(1L, 5L),
                    "suspensionLevel" to suspensionLevel.coerceIn(1L, 5L),
                    "tankLevel" to tankLevel.coerceIn(1L, 5L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateCareerProgress(
        userId: String,
        money: Long,
        xp: Long,
        level: Long,
        totalDistanceKm: Double,
        completedMissionsCount: Long,
        unlockedRouteIds: List<String>
    ): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Refusing cross-user career update" }
            require(money >= 0L && xp >= 0L && level >= 1L)
            require(unlockedRouteIds.isNotEmpty() && unlockedRouteIds.size <= 20)
            db.collection("users").document(uid).update(
                mapOf(
                    "money" to money,
                    "xp" to xp,
                    "level" to level,
                    "totalDistanceKm" to totalDistanceKm.coerceAtLeast(0.0),
                    "completedMissionsCount" to completedMissionsCount.coerceAtLeast(0L),
                    "unlockedRouteIds" to unlockedRouteIds,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addDeliveryRecord(
        userId: String,
        missionId: String,
        cargoName: String,
        originName: String,
        destinationName: String,
        truckId: String,
        distanceKm: Double,
        rewardMoney: Long,
        damagePercent: Long
    ): Result<Unit> {
        var docPath = "users/$userId/deliveries"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Refusing cross-user delivery write" }
            val docRef = db.collection("users").document(uid).collection("deliveries").document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "missionId" to missionId.take(64),
                "cargoName" to cargoName.take(80),
                "originName" to originName.take(60),
                "destinationName" to destinationName.take(60),
                "truckId" to truckId,
                "distanceKm" to distanceKm.coerceIn(0.0, 100000.0),
                "rewardMoney" to rewardMoney.coerceIn(0L, 5000000L),
                "damagePercent" to damagePercent.coerceIn(0L, 100L),
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateSettings(
        userId: String,
        driverName: String,
        graphicsQuality: GraphicsQuality,
        gearboxMode: GearboxMode,
        steeringMode: SteeringMode,
        soundEnabled: Boolean
    ): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Refusing cross-user settings update" }
            val safeName = driverName.trim().take(40).ifBlank { "Capitan Ruta" }
            db.collection("users").document(uid).update(
                mapOf(
                    "driverName" to safeName,
                    "graphicsQuality" to graphicsQuality.name,
                    "gearboxMode" to gearboxMode.name,
                    "steeringMode" to steeringMode.name,
                    "soundEnabled" to soundEnabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }
}
