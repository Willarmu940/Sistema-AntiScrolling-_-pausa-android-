package co.edu.ucentral.pausa

import android.accessibilityservice.AccessibilityService
import android.animation.ValueAnimator
import android.os.Bundle
import android.os.CountDownTimer
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import co.edu.ucentral.pausa.databinding.ActivityFrictionBinding

/** RF-04 y RF-07: pantalla de fricción. Pausa de respiración antes de poder seguir. */
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

        b.tvQuestion.text = QUESTIONS[store.interventions % QUESTIONS.size]
        b.tvInfo.text = "Llevas $sec s deslizando sin parar en $app."

        // Círculo de respiración
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
        timer = object : CountDownTimer(16_000, 1_000) {
            override fun onTick(ms: Long) {
                b.tvHint.text = "Respira con el círculo… ${ms / 1000} s"
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
            store.snoozeUntil = System.currentTimeMillis() + 5 * 60_000L
            store.log("Seguiste en $app 5 min más")
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
        private val QUESTIONS = listOf(
            "¿Qué venías a hacer aquí?",
            "¿Esto te está sumando algo ahora?",
            "¿Cómo está tu cuerpo? ¿Cuello, ojos, hombros?"
        )
    }
}
