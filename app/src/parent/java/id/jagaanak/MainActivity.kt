package id.jagaanak

import android.os.Bundle
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
import com.google.firebase.firestore.ListenerRegistration

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { Root() } }
    }

    @Composable
    fun Root() {
        val ctx = LocalContext.current
        val prefs = remember { Prefs(ctx) }
        var hasFamily by remember { mutableStateOf(prefs.familyCode != null) }
        var unlocked by remember { mutableStateOf(false) }
        when {
            !hasFamily -> SetupScreen(prefs) { hasFamily = true; unlocked = true }
            !unlocked -> PinScreen { if (prefs.checkPin(it)) unlocked = true }
            else -> Dashboard(prefs)
        }
    }

    @Composable
    fun SetupScreen(prefs: Prefs, onDone: () -> Unit) {
        var pin by remember { mutableStateOf("") }
        var busy by remember { mutableStateOf(false) }
        var err by remember { mutableStateOf<String?>(null) }
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            Arrangement.Center, Alignment.CenterHorizontally
        ) {
            Text("JagaAnak Orang Tua", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text("Buat PIN (4-6 digit). PIN dipakai untuk membuka panel ini dan membuka blokir di HP anak.")
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(pin, { pin = it.filter(Char::isDigit).take(6) },
                label = { Text("PIN orang tua") }, singleLine = true)
            err?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.height(12.dp))
            Button(enabled = pin.length >= 4 && !busy, onClick = {
                busy = true; err = null
                FamilyRepo.signIn({
                    val code = FamilyRepo.newCode()
                    FamilyRepo.createFamily(code, PinHash.of(pin), {
                        prefs.familyCode = code; prefs.setPin(pin); busy = false; onDone()
                    }, { e -> busy = false; err = e })
                }, { e -> busy = false; err = e })
            }) { Text(if (busy) "Membuat…" else "Buat kode keluarga") }
        }
    }

    @Composable
    fun PinScreen(onOk: (String) -> Unit) {
        var pin by remember { mutableStateOf("") }
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            Arrangement.Center, Alignment.CenterHorizontally
        ) {
            Text("JagaAnak Orang Tua", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(pin, { pin = it.filter(Char::isDigit).take(6) },
                label = { Text("Masukkan PIN") }, singleLine = true)
            Spacer(Modifier.height(12.dp))
            Button(enabled = pin.length >= 4, onClick = { onOk(pin); pin = "" }) { Text("Masuk") }
        }
    }

    @Composable
    fun Dashboard(prefs: Prefs) {
        val code = prefs.familyCode!!
        var st by remember { mutableStateOf<FamilyState?>(null) }
        var err by remember { mutableStateOf<String?>(null) }

        DisposableEffect(code) {
            var reg: ListenerRegistration? = null
            FamilyRepo.signIn({ reg = FamilyRepo.listen(code) { st = it } }, { err = it })
            onDispose { reg?.remove() }
        }
        val s = st

        LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text("Panel Orang Tua", style = MaterialTheme.typography.headlineSmall) }

            item {
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
                    Text("Kode keluarga (masukkan di HP anak)", style = MaterialTheme.typography.labelLarge)
                    Text(code.chunked(4).joinToString("-"), style = MaterialTheme.typography.headlineMedium)
                    if (s != null && s.childPaired) {
                        Text("✅ HP anak terhubung")
                        if (s.lastSeen > 0) {
                            val m = (System.currentTimeMillis() - s.lastSeen) / 60_000
                            Text("Terakhir aktif: ${if (m < 1) "baru saja" else "$m menit lalu"}")
                        }
                    } else Text("⏳ Menunggu HP anak memasukkan kode")
                } }
            }

            err?.let { e -> item { Text(e, color = MaterialTheme.colorScheme.error) } }

            if (s != null) {
                item {
                    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
                        Text("Batas waktu layar harian", style = MaterialTheme.typography.titleMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton({
                                FamilyRepo.updateSettings(code, (s.dailyLimitMin - 15).coerceIn(15, 720), s.blocked)
                            }) { Text("−15") }
                            Text("  ${s.dailyLimitMin / 60} j ${s.dailyLimitMin % 60} m  ")
                            OutlinedButton({
                                FamilyRepo.updateSettings(code, (s.dailyLimitMin + 15).coerceIn(15, 720), s.blocked)
                            }) { Text("+15") }
                        }
                        Text("Terpakai hari ini: ${s.totalMin} menit")
                    } }
                }
                item { Text("Aplikasi di HP anak", style = MaterialTheme.typography.titleMedium) }
                if (s.apps.isEmpty()) item { Text("Belum ada data dari HP anak.") }
                items(s.apps.sortedByDescending { it.min }, key = { it.pkg }) { a ->
                    Row(
                        Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(a.label)
                            Text("${a.min} menit hari ini", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(a.pkg in s.blocked, { chk ->
                            FamilyRepo.updateSettings(
                                code, s.dailyLimitMin,
                                if (chk) s.blocked + a.pkg else s.blocked - a.pkg
                            )
                        })
                    }
                }
            }
        }
    }
}
