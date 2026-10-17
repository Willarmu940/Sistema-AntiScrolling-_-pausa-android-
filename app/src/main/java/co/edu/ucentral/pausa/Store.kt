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

    /** Segundos de scroll continuo antes de intervenir. Demo: segundos. Versión real: minutos. */
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

    // ---- Tiempo libre (opcional): periodos sin pantalla de fricción ----
    var freeUntil: Long
        get() = p.getLong("free_until", 0L)
        set(v) { p.edit().putLong("free_until", v).apply() }

    fun freeUsesToday(): Int = p.getInt("free_uses_${LocalDate.now()}", 0)

    fun addFreeUse() {
        val k = "free_uses_${LocalDate.now()}"
        p.edit().putInt(k, p.getInt(k, 0) + 1).apply()
    }

    // ---- Perfil de hábitos (encuesta inicial, beta) ----
    var surveyDone: Boolean
        get() = p.getBoolean("sv_done", false)
        set(v) { p.edit().putBoolean("sv_done", v).apply() }

    var surveySkipped: Boolean
        get() = p.getBoolean("sv_skip", false)
        set(v) { p.edit().putBoolean("sv_skip", v).apply() }

    /** Clave del perfil dominante (noche, foco, estimulo, auto) o "" si no hay. */
    var profileKey: String
        get() = p.getString("profile", "") ?: ""
        set(v) { p.edit().putString("profile", v).apply() }

    /** 0 = bajo, 1 = moderado, 2 = alto. */
    var riskLevel: Int
        get() = p.getInt("risk", 0)
        set(v) { p.edit().putInt("risk", v).apply() }

    /** Respuestas de la encuesta separadas por coma (solo en este dispositivo). */
    var surveyAnswers: String
        get() = p.getString("answers", "") ?: ""
        set(v) { p.edit().putString("answers", v).apply() }

    // ---- Apps vigiladas (RF-02) ----
    fun isEnabled(pkg: String): Boolean = p.getBoolean("en_$pkg", true)

    fun setEnabled(pkg: String, enabled: Boolean) {
        p.edit().putBoolean("en_$pkg", enabled).apply()
    }

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
        (p.getString("log", "") ?: "").split("\n").filter { it.isNotBlank() }

    fun reset() { p.edit().clear().apply() }

    companion object {
        /** Máximo de periodos de tiempo libre por día, para que no se use como escape constante. */
        const val MAX_FREE_USES = 3
    }
}
