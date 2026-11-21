package id.jagaanak

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    private var tick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33)
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        setContent { MaterialTheme { Root(tick) } }
    }

    override fun onResume() { super.onResume(); tick++ }

    @Composable
    fun Root(refresh: Int) {
        val ctx = LocalContext.current
        val prefs = remember { Prefs(ctx) }
        var unlocked by remember { mutableStateOf(false) }
        var hasPin by remember { mutableStateOf(prefs.hasPin) }
        when {
            !hasPin -> PinScreen("Buat PIN orang tua (4-6 digit)") { prefs.setPin(it); hasPin = true; unlocked = true }
            !unlocked -> PinScreen("Masukkan PIN orang tua") { if (prefs.checkPin(it)) unlocked = true }
            else -> Dashboard(prefs, refresh)
        }
    }

    @Composable
    fun PinScreen(title: String, onOk: (String) -> Unit) {
        var pin by remember { mutableStateOf("") }
        Column(Modifier.fillMaxSize().padding(32.dp), Arrangement.Center, Alignment.CenterHorizontally) {
            Text("JagaAnak", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(pin, { pin = it.filter(Char::isDigit).take(6) },
                label = { Text(title) }, singleLine = true)
            Spacer(Modifier.height(12.dp))
            Button(enabled = pin.length >= 4, onClick = { onOk(pin); pin = "" }) { Text("Lanjut") }
        }
    }

    @Composable
    fun Dashboard(prefs: Prefs, refresh: Int) {
        val ctx = LocalContext.current
        val repo = remember { UsageRepository(ctx) }
        var limit by remember { mutableIntStateOf(prefs.dailyLimitMin) }
        var blocked by remember { mutableStateOf(prefs.blocked) }
        var on by remember { mutableStateOf(prefs.monitoring) }
        val usageOk = remember(refresh) { repo.hasUsageAccess() }
        val overlayOk = remember(refresh) { repo.hasOverlay() }
        val apps = remember(refresh) { if (usageOk) repo.launchableApps() else emptyList() }
        val usage = remember(refresh) { if (usageOk) repo.todayUsageMs() else emptyMap() }

        LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text("Panel Orang Tua", style = MaterialTheme.typography.headlineSmall) }

            item {
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
                    Text("Izin yang dibutuhkan", style = MaterialTheme.typography.titleMedium)
                    PermRow("Akses data penggunaan", usageOk) {
                        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
                    PermRow("Tampil di atas aplikasi lain", overlayOk) {
                        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName"))) }
                } }
            }

            item {
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
                    Text("Batas waktu layar harian", style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton({ limit -= 15; prefs.dailyLimitMin = limit; limit = prefs.dailyLimitMin }) { Text("−15") }
                        Text("  ${limit / 60} j ${limit % 60} m  ")
                        OutlinedButton({ limit += 15; prefs.dailyLimitMin = limit; limit = prefs.dailyLimitMin }) { Text("+15") }
                    }
                    val total = usage.filterKeys { it != packageName }.values.sum() / 60_000
                    Text("Terpakai hari ini: $total menit")
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Pengawasan aktif  ")
                        Switch(on, {
                            on = it; prefs.monitoring = it
                            if (it) MonitorService.start(ctx) else MonitorService.stop(ctx)
                        }, enabled = usageOk && overlayOk)
                    }
                } }
            }

            item { Text("Blokir aplikasi", style = MaterialTheme.typography.titleMedium) }
            items(apps, key = { it.pkg }) { a ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(a.label)
                        Text("${(usage[a.pkg] ?: 0L) / 60_000} menit hari ini",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(a.pkg in blocked, { chk ->
                        blocked = if (chk) blocked + a.pkg else blocked - a.pkg
                        prefs.blocked = blocked
                    })
                }
            }
        }
    }

    @Composable
    private fun PermRow(label: String, ok: Boolean, onGrant: () -> Unit) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${if (ok) "✅" else "⚠️"} $label")
            if (!ok) TextButton(onGrant) { Text("Aktifkan") }
        }
    }
}

