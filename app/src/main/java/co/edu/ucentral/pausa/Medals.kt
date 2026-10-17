package co.edu.ucentral.pausa

/** RF-06: medallas por cumplir metas de desconexión. */
object Medals {

    class Medal(val name: String, val description: String, val earned: (Store) -> Boolean)

    val ALL = listOf(
        Medal("Primera pausa", "Cierra la app tras un aviso") { it.closed >= 1 },
        Medal("Constante", "Acepta 3 pausas") { it.closed >= 3 },
        Medal("Imparable", "Gana 50 puntos") { it.points >= 50 },
        Medal("Autoconocimiento", "Completa la encuesta de perfil") { it.surveyDone }
    )
}
