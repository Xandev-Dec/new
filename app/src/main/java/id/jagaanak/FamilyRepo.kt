package id.jagaanak

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import java.security.SecureRandom

data class AppUsage(val pkg: String, val label: String, val min: Long)

data class FamilyState(
    val dailyLimitMin: Int = 120,
    val blocked: List<String> = emptyList(),
    val pinHash: String? = null,
    val totalMin: Long = 0,
    val apps: List<AppUsage> = emptyList(),
    val lastSeen: Long = 0,
    val childPaired: Boolean = false
)

/** Semua data keluarga disimpan di satu dokumen: families/{kode}. */
object FamilyRepo {
    private fun doc(code: String) = FirebaseFirestore.getInstance().collection("families").document(code)

    fun signIn(onReady: () -> Unit, onError: (String) -> Unit) {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) { onReady(); return }
        auth.signInAnonymously()
            .addOnSuccessListener { onReady() }
            .addOnFailureListener { onError(it.message ?: "Gagal masuk ke server") }
    }

    fun newCode(): String {
        val a = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
        val r = SecureRandom()
        return (1..8).map { a[r.nextInt(a.length)] }.joinToString("")
    }

    fun createFamily(code: String, pinHash: String, onDone: () -> Unit, onError: (String) -> Unit) {
        doc(code).set(
            mapOf(
                "dailyLimitMin" to 120, "blocked" to emptyList<String>(),
                "pinHash" to pinHash, "childPaired" to false,
                "createdAt" to System.currentTimeMillis()
            )
        ).addOnSuccessListener { onDone() }.addOnFailureListener { onError(it.message ?: "Gagal membuat keluarga") }
    }

    fun exists(code: String, cb: (Boolean) -> Unit, onError: (String) -> Unit) {
        doc(code).get().addOnSuccessListener { cb(it.exists()) }
            .addOnFailureListener { onError(it.message ?: "Gagal memeriksa kode") }
    }

    fun markChildPaired(code: String) {
        doc(code).set(mapOf("childPaired" to true), SetOptions.merge())
    }

    fun listen(code: String, onState: (FamilyState) -> Unit): ListenerRegistration =
        doc(code).addSnapshotListener { snap, _ ->
            if (snap != null && snap.exists()) onState(parse(snap))
        }

    fun updateSettings(code: String, limitMin: Int, blocked: List<String>) {
        doc(code).update(mapOf("dailyLimitMin" to limitMin, "blocked" to blocked))
    }

    fun pushUsage(code: String, totalMin: Long, apps: List<AppUsage>) {
        doc(code).set(
            mapOf(
                "totalMin" to totalMin,
                "apps" to apps.map { mapOf("pkg" to it.pkg, "label" to it.label, "min" to it.min) },
                "lastSeen" to System.currentTimeMillis()
            ), SetOptions.merge()
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun parse(s: DocumentSnapshot): FamilyState {
        val apps = (s.get("apps") as? List<Map<String, Any>>).orEmpty().map {
            AppUsage(it["pkg"] as String, it["label"] as? String ?: "", (it["min"] as? Number)?.toLong() ?: 0L)
        }
        return FamilyState(
            dailyLimitMin = s.getLong("dailyLimitMin")?.toInt() ?: 120,
            blocked = (s.get("blocked") as? List<String>).orEmpty(),
            pinHash = s.getString("pinHash"),
            totalMin = s.getLong("totalMin") ?: 0L,
            apps = apps,
            lastSeen = s.getLong("lastSeen") ?: 0L,
            childPaired = s.getBoolean("childPaired") ?: false
        )
    }
}
