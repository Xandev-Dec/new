package id.jagaanak

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class BlockActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = Prefs(this)
        val reason = intent.getStringExtra("reason") ?: "Akses dibatasi."
        setContent {
            MaterialTheme {
                var pin by remember { mutableStateOf("") }
                var err by remember { mutableStateOf(false) }
                Column(
                    Modifier.fillMaxSize().padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Waktunya istirahat 🌙", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(12.dp))
                    Text(reason)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { goHome() }) { Text("Kembali ke Beranda") }
                    Spacer(Modifier.height(32.dp))
                    Text("Izin orang tua (buka 15 menit):", style = MaterialTheme.typography.labelLarge)
                    OutlinedTextField(
                        value = pin, onValueChange = { pin = it.filter(Char::isDigit).take(6); err = false },
                        label = { Text("PIN orang tua") }, isError = err, singleLine = true
                    )
                    Button(onClick = {
                        if (prefs.checkPin(pin)) {
                            prefs.unlockUntil = System.currentTimeMillis() + 15 * 60_000
                            finish()
                        } else err = true
                    }) { Text("Buka") }
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() = goHome()

    private fun goHome() {
        startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }
}
