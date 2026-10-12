package co.edu.ucentral.pausa

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import co.edu.ucentral.pausa.databinding.ActivityMainBinding
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private lateinit var store: Store

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        store = Store(this)

        b.btnA11y.setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        b.btnUsage.setOnClickListener { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
        b.btnDemo.setOnClickListener { startActivity(Intent(this, DemoFeedActivity::class.java)) }
        b.btnReset.setOnClickListener { store.reset(); render() }

        // RF-02 (parcial): umbral configurable entre 10 y 60 segundos
        b.seekTh.progress = store.thresholdSec - 10
        b.seekTh.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                if (fromUser) { store.thresholdSec = p + 10; render() }
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        val a11y = isA11yEnabled()
        val usage = hasUsageAccess()
        b.tvStatusA11y.text = if (a11y) "Detección de scroll: activa" else "Detección de scroll: desactivada"
        b.tvStatusUsage.text = if (usage) "Acceso al tiempo de uso: concedido" else "Acceso al tiempo de uso: pendiente"
        b.btnA11y.isEnabled = !a11y
        b.btnUsage.isEnabled = !usage

        // RF-01: tiempo en pantalla según el sistema + tiempo deslizando según nuestro servicio
        val fg = if (usage) foregroundToday() else emptyMap()
        var totalMs = 0L
        val lines = Watched.apps.map { (pkg, name) ->
            val screen = fg[pkg] ?: 0L
            val scroll = store.scrollMs(pkg)
            totalMs += maxOf(screen, scroll)
            "$name — ${fmt(screen)} en pantalla · ${fmt(scroll)} deslizando"
        }
        b.tvApps.text = lines.joinToString("\n")
        b.tvTotal.text = fmt(totalMs)
        b.progress.progress = (totalMs * 100 / (store.goalMin * 60_000L)).coerceAtMost(100).toInt()

        b.tvPoints.text = "Puntos: ${store.points} · Pausas aceptadas: ${store.closed}"
        b.tvTh.text = "Alerta tras ${store.thresholdSec} s de scroll continuo (demo; en producción 20 min)"
        b.tvLog.text = store.logs().joinToString("\n")
    }

    private fun fmt(ms: Long): String =
        if (ms < 60_000) "${ms / 1000} s" else "${ms / 60_000} min"

    private fun foregroundToday(): Map<String, Long> {
        val usm = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val start = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, System.currentTimeMillis())
            .filter { it.packageName in Watched.apps }
            .groupBy { it.packageName }
            .mapValues { (_, l) -> l.sumOf { it.totalTimeInForeground } }
    }

    @Suppress("DEPRECATION")
    private fun hasUsageAccess(): Boolean {
        val ops = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        val mode = ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun isA11yEnabled(): Boolean {
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        return enabled.contains("$packageName/${ScrollWatcherService::class.java.name}")
    }
}
