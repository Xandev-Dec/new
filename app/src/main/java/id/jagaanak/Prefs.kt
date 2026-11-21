package id.jagaanak

import android.content.Context
import java.security.MessageDigest

object PinHash {
    fun of(pin: String): String =
        MessageDigest.getInstance("SHA-256").digest(("jagaanak:$pin").toByteArray())
            .joinToString("") { "%02x".format(it) }
}

class Prefs(ctx: Context) {
    private val p = ctx.applicationContext.getSharedPreferences("jagaanak", Context.MODE_PRIVATE)

    var familyCode: String?
        get() = p.getString("code", null)
        set(v) { p.edit().putString("code", v).apply() }

    var paired: Boolean
        get() = p.getBoolean("paired", false)
        set(v) { p.edit().putBoolean("paired", v).apply() }

    var pinHash: String?
        get() = p.getString("pinHash", null)
        set(v) { p.edit().putString("pinHash", v).apply() }

    fun setPin(pin: String) { pinHash = PinHash.of(pin) }
    fun checkPin(pin: String) = pinHash != null && pinHash == PinHash.of(pin)

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
}
