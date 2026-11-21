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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
        var paired by remember { mutableStateOf(prefs.paired) }
        if (!paired) PairScreen(prefs) { paired = true }
        else StatusScreen(prefs, refresh) { paired = false }
    }

    @Composable
    fun PairScreen(prefs: Prefs, onPaired: () -> Unit) {
        var code by remember { mutableStateOf("") }
        var agree by remember { mutableStateOf(false) }
        var msg by remember { mutableStateOf<String?>(null) }
        var busy by remember { mutableStateOf(false) }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("JagaAnak untuk Anak", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Aplikasi ini menghubungkan HP ini ke HP orang tua.\n\n" +
                "Yang dibagikan ke orang tua: daftar aplikasi dan lama pemakaiannya hari ini.\n" +
                "Yang TIDAK dibaca: pesan, foto, password, lokasi, dan isi layar.\n\n" +
                "Orang tua bisa mengatur batas waktu layar dan memblokir aplikasi. " +
                "Notifikasi \"JagaAnak aktif\" akan selalu terlihat selama pengawasan berjalan."
            )
            OutlinedTextField(
                code, { code = it.uppercase().filter(Char::isLetterOrDigit).take(8) },
                label = { Text("Kode dari HP orang tua (8 karakter)") }, singleLine = true
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(agree, { agree = it })
                Text("Saya mengerti dan setuju")
            }
            msg?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(enabled = agree && code.length == 8 && !busy, onClick = {
                busy = true; msg = null
                FamilyRepo.signIn({
                    FamilyRepo.exists(code, { ok ->
                        busy = false
                        if (ok) {
                            prefs.familyCode = code; prefs.paired = true
                            FamilyRepo.markChildPaired(code); onPaired()
                        } else msg = "Kode tidak ditemukan. Periksa lagi."
                    }, { e -> busy = false; msg = e })
                }, { e -> busy = false; msg = e })
            }) { Text(if (busy) "Menghubungkan…" else "Hubungkan") }
        }
    }

    @Composable
    fun StatusScreen(prefs: Prefs, refresh: Int, onUnpaired: () -> Unit) {
        val ctx = LocalContext.current
        val repo = remember { UsageRepository(ctx) }
        val usageOk = remember(refresh) { repo.hasUsageAccess() }
        val overlayOk = remember(refresh) { repo.hasOverlay() }
        var pin by remember { mutableStateOf("") }
        var err by remember { mutableStateOf(false) }

        LaunchedEffect(usageOk, overlayOk) {
            if (usageOk && overlayOk) { prefs.monitoring = true; MonitorService.start(ctx) }
        }

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("JagaAnak aktif", style = MaterialTheme.typography.headlineMedium)
            Text("Terhubung ke keluarga: ${prefs.familyCode}")
            PermRow("Akses data penggunaan", usageOk) {
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
            PermRow("Tampil di atas aplikasi lain", overlayOk) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            }
            Text(
                "Orang tua melihat daftar aplikasi dan lama pemakaiannya. " +
                "Pesan, foto, password, dan lokasi tidak dibaca.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(24.dp))
            Text("Lepas sambungan (butuh PIN orang tua)", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(
                pin, { pin = it.filter(Char::isDigit).take(6); err = false },
                label = { Text("PIN orang tua") }, isError = err, singleLine = true
            )
            Button(onClick = {
                if (prefs.checkPin(pin)) {
                    MonitorService.stop(ctx); prefs.monitoring = false
                    prefs.paired = false; prefs.familyCode = null; onUnpaired()
                } else err = true
            }) { Text("Lepas sambungan") }
        }
    }

    @Composable
    private fun PermRow(label: String, ok: Boolean, onGrant: () -> Unit) {
        Row(
            Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("${if (ok) "✅" else "⚠️"} $label")
            if (!ok) TextButton(onGrant) { Text("Aktifkan") }
        }
    }
}
