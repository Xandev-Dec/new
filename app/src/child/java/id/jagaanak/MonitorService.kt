package id.jagaanak

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.ListenerRegistration

class MonitorService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var prefs: Prefs
    private lateinit var repo: UsageRepository
    private var lastBlockAt = 0L
    private var reg: ListenerRegistration? = null

    private val tick = object : Runnable {
        override fun run() { check(); handler.postDelayed(this, 1500) }
    }
    private val pushTick = object : Runnable {
        override fun run() { push(); handler.postDelayed(this, 60_000) }
    }

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this); repo = UsageRepository(this)
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel("monitor", "Pengawasan aktif", NotificationManager.IMPORTANCE_LOW)
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Notifikasi permanen: anak selalu tahu pengawasan sedang aktif.
        val n = Notification.Builder(this, "monitor")
            .setContentTitle("JagaAnak aktif")
            .setContentText("Batas waktu layar & aplikasi diatur oleh orang tua.")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setOngoing(true).build()
        startForeground(1, n)
        handler.removeCallbacks(tick); handler.post(tick)
        startSync()
        return START_STICKY
    }

    private fun startSync() {
        val code = prefs.familyCode ?: return
        FamilyRepo.signIn({
            reg?.remove()
            reg = FamilyRepo.listen(code) { s ->
                prefs.dailyLimitMin = s.dailyLimitMin
                prefs.blocked = s.blocked.toSet()
                prefs.pinHash = s.pinHash
            }
        }, {})
        handler.removeCallbacks(pushTick); handler.post(pushTick)
    }

    private fun totalMinutes(usage: Map<String, Long>): Long {
        val home = repo.homePackage()
        return usage.filterKeys { it != packageName && it != home }.values.sum() / 60_000
    }

    private fun push() {
        val code = prefs.familyCode ?: return
        if (!repo.hasUsageAccess()) return
        val usage = repo.todayUsageMs()
        val home = repo.homePackage()
        val apps = repo.launchableApps().filter { it.pkg != home }
            .map { AppUsage(it.pkg, it.label, (usage[it.pkg] ?: 0L) / 60_000) }
        FamilyRepo.pushUsage(code, totalMinutes(usage), apps)
    }

    private fun check() {
        if (!repo.hasUsageAccess() || !repo.hasOverlay()) return
        if (System.currentTimeMillis() < prefs.unlockUntil) return
        val fg = repo.foregroundPackage() ?: return
        if (fg == packageName || fg == repo.homePackage() || fg == "com.android.systemui") return

        val overLimit = totalMinutes(repo.todayUsageMs()) >= prefs.dailyLimitMin
        val isBlocked = fg in prefs.blocked
        if ((overLimit || isBlocked) && System.currentTimeMillis() - lastBlockAt > 2000) {
            lastBlockAt = System.currentTimeMillis()
            startActivity(
                Intent(this, BlockActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra("reason", if (isBlocked) "Aplikasi ini diblokir oleh orang tua."
                    else "Batas waktu layar hari ini sudah habis.")
            )
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick); handler.removeCallbacks(pushTick)
        reg?.remove(); super.onDestroy()
    }
    override fun onBind(i: Intent?): IBinder? = null

    companion object {
        fun start(c: Context) =
            ContextCompat.startForegroundService(c, Intent(c, MonitorService::class.java))
        fun stop(c: Context) = c.stopService(Intent(c, MonitorService::class.java))
    }
}
