package co.edu.ucentral.pausa

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import co.edu.ucentral.pausa.databinding.ActivityMainBinding
import co.edu.ucentral.pausa.databinding.ItemAppBinding
import co.edu.ucentral.pausa.databinding.ItemToggleBinding
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
        b.btnSurvey.setOnClickListener { startActivity(Intent(this, OnboardingActivity::class.java)) }
        b.btnReset.setOnClickListener {
            store.reset()
            buildToggles()
            render()
        }

        // RF-02: umbral de scroll continuo configurable (10 a 60 s en la demo)
        b.seekTh.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                if (fromUser) { store.thresholdSec = p + 10; render() }
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        // RF-02: meta diaria configurable (15 a 240 min, de 15 en 15)
        b.seekGoal.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                if (fromUser) { store.goalMin = (p + 1) * 15; render() }
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        buildToggles()

        // Primera vez: ofrecer la encuesta de perfil
        if (!store.surveyDone && !store.surveySkipped) {
            startActivity(Intent(this, OnboardingActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    /** RF-02: un interruptor por app para elegir cuáles se vigilan. */
    private fun buildToggles() {
        b.llToggles.removeAllViews()
        Watched.apps.forEach { (pkg, name) ->
            val row = ItemToggleBinding.inflate(layoutInflater, b.llToggles, false)
            row.tvAppName.text = name
            row.sw.isChecked = store.isEnabled(pkg)
            row.sw.setOnCheckedChangeListener { _, checked -> store.setEnabled(pkg, checked) }
            b.llToggles.addView(row.root)
        }
    }

    private fun render() {
        val a11y = isA11yEnabled()
        val usage = hasUsageAccess()

        // Permisos: si ya están concedidos, se oculta el botón
        b.tvStatusA11y.text = if (a11y) "Detección de scroll: activa ✓" else "Detección de scroll: desactivada"
        b.tvStatusUsage.text = if (usage) "Acceso al tiempo de uso: concedido ✓" else "Acceso al tiempo de uso: pendiente"
        b.btnA11y.visibility = if (a11y) View.GONE else View.VISIBLE
        b.btnUsage.visibility = if (usage) View.GONE else View.VISIBLE

        // RF-01: tiempo en pantalla (sistema) + tiempo deslizando (nuestro servicio)
        val fg = if (usage) foregroundToday() else emptyMap()
        val rows = Watched.apps
            .map { (pkg, name) -> Triple(name, fg[pkg] ?: 0L, store.scrollMs(pkg)) }
            .filter { it.second > 0 || it.third > 0 }
            .sortedByDescending { maxOf(it.second, it.third) }

        val totalMs = rows.sumOf { maxOf(it.second, it.third) }
        val peak = rows.maxOfOrNull { maxOf(it.second, it.third) } ?: 1L

        b.llApps.removeAllViews()
        rows.forEach { (name, screen, scroll) ->
            val item = ItemAppBinding.inflate(layoutInflater, b.llApps, false)
            item.tvName.text = name
            item.tvDetail.text = "${fmt(screen)} en pantalla · ${fmt(scroll)} deslizando"
            item.bar.progress = (maxOf(screen, scroll) * 100 / peak).toInt()
            b.llApps.addView(item.root)
        }
        b.tvEmpty.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE

        b.tvTotal.text = fmt(totalMs)
        b.tvGoal.text = "de ${store.goalMin} min de meta diaria"
        b.progress.progress = (totalMs * 100 / (store.goalMin * 60_000L)).coerceAtMost(100).toInt()

        // Perfil de hábitos (beta)
        val profile = Profiles.byKey(store.profileKey)
        if (profile != null) {
            b.tvProfileName.text = "${profile.name} · uso ${Profiles.levelName(store.riskLevel)}"
            b.tvProfileDesc.text = profile.tip
            b.btnSurvey.text = "Rehacer la encuesta"
        } else {
            b.tvProfileName.text = "Aún sin perfil"
            b.tvProfileDesc.text = "Responde 8 preguntas cortas para personalizar los avisos."
            b.btnSurvey.text = "Hacer la encuesta"
        }

        // Recompensas y medallas (RF-06)
        b.tvPoints.text = store.points.toString()
        b.tvClosed.text = "${store.closed} pausas\naceptadas"
        b.tvMedals.text = Medals.ALL.joinToString("\n") { m ->
            (if (m.earned(store)) "🏅 " else "🔒 ") + m.name + " — " + m.description
        }

        // Límites
        b.tvTh.text = "Intervenir tras ${store.thresholdSec} s de scroll continuo"
        b.seekTh.progress = store.thresholdSec - 10
        b.tvGoalSet.text = "Meta diaria: ${store.goalMin} min"
        b.seekGoal.progress = (store.goalMin / 15 - 1).coerceIn(0, 15)

        val log = store.logs()
        b.tvLog.text = if (log.isEmpty()) "Aún no hay intervenciones." else log.joinToString("\n")
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
