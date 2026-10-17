package co.edu.ucentral.pausa

/**
 * Perfil de hábitos digitales (beta). NO es una prueba psicológica ni un diagnóstico:
 * son 8 preguntas propias, inspiradas en los casos del estudio de campo, que sirven
 * para personalizar los avisos. Escala 1 (nunca) a 5 (siempre).
 */
object Profiles {

    val QUESTIONS = listOf(
        "Abro redes sociales casi por reflejo, sin haberlo decidido.",                    // 0 automático
        "Lo que iba a ser “cinco minutos” termina siendo mucho más tiempo.",              // 1 automático
        "Uso el celular en la cama y eso me quita o retrasa el sueño.",                   // 2 noche
        "Cuando debo estudiar o trabajar, termino revisando redes sociales.",             // 3 foco
        "Si me aburro o me bloqueo, lo primero que hago es abrir una red social.",        // 4 estímulo
        "Me incomoda perderme lo que pasa si no reviso mis redes.",                       // 5 estímulo
        "Me he propuesto usar menos el celular y no lo he cumplido.",                     // 6 control
        "Los límites o avisos de tiempo del celular los omito o los desactivo."           // 7 control
    )

    class Profile(
        val key: String,
        val name: String,
        val description: String,
        val tip: String,
        val questions: List<String>
    )

    class Result(val profile: Profile, val level: Int, val score: Int, val thresholdSec: Int)

    val ALL = listOf(
        Profile(
            "noche", "Nocturno",
            "Tu momento más difícil es la noche: el scroll te roba horas de sueño.",
            "Tus avisos se enfocan en el descanso. Prueba dejar el celular fuera de la cama.",
            listOf(
                "Es tarde para tu cuerpo. ¿Esto te ayuda a dormir mejor?",
                "¿Cómo te vas a sentir mañana si duermes menos por esto?",
                "¿Puedes dejar el celular lejos de la cama esta noche?"
            )
        ),
        Profile(
            "foco", "Disperso",
            "El scroll te interrumpe cuando debes estudiar o trabajar.",
            "Tus avisos te devuelven a la tarea. Prueba bloques de 25 minutos sin celular.",
            listOf(
                "¿Esto es lo que debías estar haciendo ahora?",
                "¿Qué tarea quedó esperando mientras deslizabas?",
                "¿Puedes volver a lo tuyo con 25 minutos de enfoque?"
            )
        ),
        Profile(
            "estimulo", "Buscador de estímulo",
            "Abres redes por aburrimiento o por miedo a perderte algo.",
            "Tus avisos preguntan qué necesitas de verdad. Ten una alternativa a mano.",
            listOf(
                "¿Estás aburrido o te da miedo perderte algo?",
                "¿Qué necesitas de verdad en este momento?",
                "Lo importante te va a llegar igual. ¿Seguimos?"
            )
        ),
        Profile(
            "auto", "Automático",
            "Abres y deslizas por reflejo, casi sin decidirlo.",
            "Tus avisos buscan que elijas de forma consciente en vez de por reflejo.",
            listOf(
                "¿Elegiste abrir esto o fue un reflejo?",
                "¿Cuántos videos recuerdas de los últimos 10 minutos?",
                "¿Qué venías a hacer aquí?"
            )
        )
    )

    fun byKey(key: String): Profile? = ALL.firstOrNull { it.key == key }

    fun levelName(level: Int): String = when (level) {
        2 -> "alto"
        1 -> "moderado"
        else -> "bajo"
    }

    /** answers: 8 valores entre 1 y 5, en el orden de [QUESTIONS]. */
    fun evaluate(answers: IntArray): Result {
        val total = answers.sum()
        val scores = listOf(
            "noche" to answers[2].toDouble(),
            "foco" to answers[3].toDouble(),
            "estimulo" to (answers[4] + answers[5]) / 2.0,
            "auto" to (answers[0] + answers[1]) / 2.0
        )
        val bestKey = scores.maxByOrNull { it.second }?.first ?: "auto"
        val profile = byKey(bestKey) ?: ALL.last()

        val level = when {
            total >= 30 -> 2
            total >= 20 -> 1
            else -> 0
        }
        // Mientras más riesgo, antes interviene la app (demo en segundos; versión real en minutos)
        val threshold = when (level) {
            2 -> 10
            1 -> 20
            else -> 30
        }
        return Result(profile, level, total, threshold)
    }
}
