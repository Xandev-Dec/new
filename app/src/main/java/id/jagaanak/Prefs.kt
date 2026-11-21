package id.jagaanak

import android.content.Context
import java.security.MessageDigest

class Prefs(ctx: Context) {
    private val p = ctx.applicationContext.getSharedPreferences("jagaanak", Context.MODE_PRIVATE)

    val hasPin: Boolean get() = p.getString("pin", null) != null
    fun setPin(pin: String) { p.edit().putString("pin", hash(pin)).apply() }
    fun checkPin(pin: String) = p.getString("pin", null) == hash(pin)

    var dailyLimitMin: Int
        get() = p.getInt("limit", 120)
        set(v) { p.edit().putInt("limit", v.coerceIn(15, 720)).apply() }

    var blocked: Set<String>
        get() = p.getStringSet("blocked", emptySet())!!.toSet()
        set(v) { p.edit().putStringSet("blocked", v).apply() }

    var unlockUntil: Long
        get() = p.getLong("unlockUntil", 0L)
        set(v) { p.edit().putLong("unlockUntil", v).apply() }

    var monitoring: Boolean
        get() = p.getBoolean("monitoring", false)
        set(v) { p.edit().putBoolean("monitoring", v).apply() }

    private fun hash(s: String) =
        MessageDigest.getInstance("SHA-256").digest(("jagaanak:$s").toByteArray())
            .joinToString("") { "%02x".format(it) }
}
