package co.edu.ucentral.pausa

import android.content.Context
import java.time.LocalDate

/** Apps vigiladas (paquete -> nombre). RF-01. */
object Watched {
    const val SELF = "co.edu.ucentral.pausa"
    val apps = linkedMapOf(
        "com.zhiliaoapp.musically" to "TikTok",
        "com.instagram.android" to "Instagram",
        "com.google.android.youtube" to "YouTube",
        "com.twitter.android" to "X",
        "com.reddit.frontpage" to "Reddit",
        SELF to "Feed de demo"
    )
}

/** Almacenamiento local básico (RF-09). Sin servidor: los datos no salen del teléfono (RNF-04). */
class Store(context: Context) {
    private val p = context.getSharedPreferences("pausa", Context.MODE_PRIVATE)

    /** Segundos de scroll continuo antes de intervenir. Demo: 20 s. Producción: 1200 s (20 min). */
    var thresholdSec: Int
        get() = p.getInt("th", 20)
        set(v) { p.edit().putInt("th", v).apply() }

    var goalMin: Int
        get() = p.getInt("goal", 60)
        set(v) { p.edit().putInt("goal", v).apply() }

    var points: Int
        get() = p.getInt("pts", 0)
        set(v) { p.edit().putInt("pts", v).apply() }

    var closed: Int
        get() = p.getInt("closed", 0)
        set(v) { p.edit().putInt("closed", v).apply() }

    var interventions: Int
        get() = p.getInt("ints", 0)
        set(v) { p.edit().putInt("ints", v).apply() }

    var snoozeUntil: Long
        get() = p.getLong("snooze", 0L)
        set(v) { p.edit().putLong("snooze", v).apply() }

    private fun day() = LocalDate.now().toString()

    fun addUsage(pkg: String, ms: Long) {
        val k = "u_${day()}_$pkg"
        p.edit().putLong(k, p.getLong(k, 0L) + ms).apply()
    }

    /** Milisegundos que el usuario pasó deslizando en [pkg] hoy. */
    fun scrollMs(pkg: String): Long = p.getLong("u_${day()}_$pkg", 0L)

    fun log(line: String) {
        val all = (listOf(line) + logs()).take(8)
        p.edit().putString("log", all.joinToString("\n")).apply()
    }

    fun logs(): List<String> =
        p.getString("log", "")!!.split("\n").filter { it.isNotBlank() }

    fun reset() { p.edit().clear().apply() }
}
