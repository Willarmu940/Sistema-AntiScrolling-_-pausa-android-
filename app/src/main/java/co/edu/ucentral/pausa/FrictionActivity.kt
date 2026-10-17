package co.edu.ucentral.pausa

import android.accessibilityservice.AccessibilityService
import android.animation.ValueAnimator
import android.os.Bundle
import android.os.CountDownTimer
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import co.edu.ucentral.pausa.databinding.ActivityFrictionBinding

/**
 * RF-04, RF-05 y RF-07: pantalla de fricción.
 * La pregunta depende del perfil del usuario (encuesta) y el ejercicio rota entre
 * respiración, estiramiento, postura y descanso visual.
 */
class FrictionActivity : AppCompatActivity() {

    private lateinit var b: ActivityFrictionBinding
    private var timer: CountDownTimer? = null
    private var anim: ValueAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityFrictionBinding.inflate(layoutInflater)
        setContentView(b.root)

        val store = Store(this)
        val pkg = intent.getStringExtra(EXTRA_PKG) ?: ""
        val sec = intent.getIntExtra(EXTRA_SECONDS, 0)
        val app = Watched.apps[pkg] ?: pkg
        val n = store.interventions

        // Texto de la pausa de gracia, según el valor de SNOOZE_MS
        val graceLabel = if (SNOOZE_MS >= 60_000L) "${SNOOZE_MS / 60_000L} minutos" else "${SNOOZE_MS / 1000L} segundos"
        b.btnContinue.text = "Seguir $graceLabel más"

        // Pregunta personalizada según el perfil (si hizo la encuesta)
        val questions = Profiles.byKey(store.profileKey)?.questions ?: DEFAULT_QUESTIONS
        b.tvQuestion.text = questions[n % questions.size]
        b.tvInfo.text = "Llevas $sec s deslizando sin parar en $app."

        // RF-05: ejercicio guiado que rota
        val exercise = EXERCISES[n % EXERCISES.size]

        // Círculo de respiración (marca el ritmo del ejercicio)
        anim = ValueAnimator.ofFloat(0.55f, 1f).apply {
            duration = 4000
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                val v = it.animatedValue as Float
                b.circle.scaleX = v
                b.circle.scaleY = v
            }
            start()
        }

        // RF-07: "atrás" no cierra la pantalla
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {}
        })

        b.btnContinue.isEnabled = false
        b.tvHint.text = exercise
        timer = object : CountDownTimer(16_000, 1_000) {
            override fun onTick(ms: Long) {
                b.tvHint.text = "$exercise\n${ms / 1000} s"
            }
            override fun onFinish() {
                b.tvHint.text = "Listo. Ahora decide."
                b.btnContinue.isEnabled = true
            }
        }.start()

        b.btnClose.setOnClickListener {
            store.points = store.points + 10          // RF-06
            store.closed = store.closed + 1
            store.log("Cerraste $app (+10 puntos)")
            ScrollWatcherService.instance?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
            finish()
        }
        b.btnContinue.setOnClickListener {
            // Durante este tiempo el servicio no vuelve a intervenir
            store.snoozeUntil = System.currentTimeMillis() + SNOOZE_MS
            store.log("Seguiste en $app $graceLabel más")
            finish()
        }
    }

    override fun onDestroy() {
        timer?.cancel()
        anim?.cancel()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_PKG = "pkg"
        const val EXTRA_SECONDS = "seconds"

        /**
         * Pausa de gracia tras elegir "Seguir".
         * Demo: 30 segundos (para poder ver la segunda intervención).
         * Versión real: 5 * 60_000L (5 minutos).
         */
        const val SNOOZE_MS = 30_000L

        private val DEFAULT_QUESTIONS = listOf(
            "¿Qué venías a hacer aquí?",
            "¿Esto te está sumando algo ahora?",
            "¿Cómo está tu cuerpo? ¿Cuello, ojos, hombros?"
        )

        private val EXERCISES = listOf(
            "Respira con el círculo: inhala al crecer, exhala al encoger.",
            "Estira el cuello: inclina la cabeza hacia un hombro y cambia de lado.",
            "Revisa tu postura: espalda recta, hombros abajo y la pantalla a la altura de los ojos.",
            "Descansa la vista: mira un punto lejano y parpadea despacio."
        )
    }
}
