package co.edu.ucentral.pausa

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/** Feed infinito simulado para demostrar RF-03 y RF-04 sin depender de TikTok/Instagram. */
class DemoFeedActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val temas = listOf(
            "Rutina de 5 minutos que cambió mi mañana",
            "Lo que nadie te dice de trabajar desde casa",
            "Este gato no se cansa de caer",
            "Receta con 3 ingredientes",
            "Tendencia del día: todos hacen esto",
            "Opinión impopular sobre el café"
        )
        val list = ListView(this).apply {
            fitsSystemWindows = true
            adapter = object : BaseAdapter() {
                override fun getCount() = Int.MAX_VALUE          // "infinito"
                override fun getItem(position: Int) = position
                override fun getItemId(position: Int) = position.toLong()
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val tv = (convertView as? TextView) ?: TextView(this@DemoFeedActivity).apply {
                        setPadding(40, 60, 40, 60)
                        textSize = 18f
                    }
                    tv.text = "@usuario${position + 1}\n${temas[position % temas.size]}"
                    return tv
                }
            }
        }
        setContentView(list)
    }
}
