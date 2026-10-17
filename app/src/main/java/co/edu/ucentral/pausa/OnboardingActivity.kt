package co.edu.ucentral.pausa

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import co.edu.ucentral.pausa.databinding.ActivityOnboardingBinding

/**
 * Encuesta inicial (beta): consentimiento -> 8 preguntas -> perfil de hábitos.
 * Los resultados se guardan solo en el dispositivo y personalizan los avisos.
 */
class OnboardingActivity : AppCompatActivity() {

    private lateinit var b: ActivityOnboardingBinding
    private lateinit var store: Store
    private val total = Profiles.QUESTIONS.size
    private val answers = IntArray(total)

    /** -1 = consentimiento, 0 until total = preguntas, total = resultado. */
    private var step = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(b.root)
        store = Store(this)

        b.cbConsent.setOnCheckedChangeListener { _, checked ->
            if (step == -1) b.btnNext.isEnabled = checked
        }
        b.rg.setOnCheckedChangeListener { _, id ->
            if (step in 0 until total && id != -1) b.btnNext.isEnabled = true
        }
        b.btnNext.setOnClickListener { onNext() }
        b.btnSkip.setOnClickListener {
            store.surveySkipped = true
            finish()
        }
        show()
    }

    private fun onNext() {
        when {
            step == -1 -> { step = 0; show() }
            step < total -> {
                answers[step] = readAnswer()
                step++
                if (step == total) saveResult()
                show()
            }
            else -> finish()
        }
    }

    private fun readAnswer(): Int = when (b.rg.checkedRadioButtonId) {
        b.rb1.id -> 1
        b.rb2.id -> 2
        b.rb3.id -> 3
        b.rb4.id -> 4
        b.rb5.id -> 5
        else -> 3
    }

    private fun saveResult() {
        val r = Profiles.evaluate(answers)
        store.profileKey = r.profile.key
        store.riskLevel = r.level
        store.thresholdSec = r.thresholdSec      // el aviso se ajusta al nivel
        store.surveyAnswers = answers.joinToString(",")
        store.surveyDone = true
        store.log("Perfil: ${r.profile.name} (nivel ${Profiles.levelName(r.level)})")
    }

    private fun show() {
        when {
            step == -1 -> {
                b.tvTitle.text = "Antes de empezar"
                b.tvBody.text = "Pausa te hará 8 preguntas cortas sobre tus hábitos con las redes " +
                    "sociales para personalizar los avisos.\n\n" +
                    "No es una prueba psicológica ni un diagnóstico. Tus respuestas se guardan solo en " +
                    "este celular, no se envían a ningún servidor y puedes borrarlas con " +
                    "“Borrar datos de prueba”. Responder es voluntario y puedes omitirlo."
                b.tvStep.visibility = View.GONE
                b.prog.visibility = View.GONE
                b.rg.visibility = View.GONE
                b.cbConsent.visibility = View.VISIBLE
                b.btnSkip.visibility = View.VISIBLE
                b.btnNext.text = "Continuar"
                b.btnNext.isEnabled = b.cbConsent.isChecked
            }
            step < total -> {
                b.tvTitle.text = "Tus hábitos"
                b.tvStep.text = "Pregunta ${step + 1} de $total"
                b.tvBody.text = Profiles.QUESTIONS[step]
                b.tvStep.visibility = View.VISIBLE
                b.prog.visibility = View.VISIBLE
                b.prog.progress = step * 100 / total
                b.cbConsent.visibility = View.GONE
                b.rg.visibility = View.VISIBLE
                b.rg.clearCheck()
                b.btnSkip.visibility = View.VISIBLE
                b.btnNext.text = if (step == total - 1) "Ver mi perfil" else "Siguiente"
                b.btnNext.isEnabled = false
            }
            else -> {
                val p = Profiles.byKey(store.profileKey)
                b.tvTitle.text = "Tu perfil: ${p?.name ?: ""}"
                b.tvBody.text = (p?.description ?: "") + "\n\n" + (p?.tip ?: "") +
                    "\n\nNivel de uso: ${Profiles.levelName(store.riskLevel)}. " +
                    "El aviso se ajustó a ${store.thresholdSec} s de scroll continuo (demo; en la " +
                    "versión real serían minutos).\n\n" +
                    "Esto no es un diagnóstico: es una guía para personalizar Pausa. " +
                    "Puedes rehacer la encuesta cuando quieras."
                b.tvStep.visibility = View.GONE
                b.prog.visibility = View.GONE
                b.rg.visibility = View.GONE
                b.cbConsent.visibility = View.GONE
                b.btnSkip.visibility = View.GONE
                b.btnNext.text = "Empezar"
                b.btnNext.isEnabled = true
            }
        }
    }
}
