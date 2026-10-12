package co.edu.ucentral.pausa

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent

/**
 * RF-03: detecta sesiones de scroll continuo.
 * Una "racha" continúa mientras los eventos de scroll lleguen con menos de IDLE_MS entre sí.
 * Al superar el umbral configurado se lanza la pantalla de fricción (RF-04).
 * Solo se miden eventos de scroll; no se lee el contenido de la pantalla.
 */
class ScrollWatcherService : AccessibilityService() {

    private val store by lazy { Store(this) }
    private var streakStart = 0L
    private var lastScroll = 0L
    private var lastPkg = ""

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onInterrupt() {}

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED) return
        val pkg = event.packageName?.toString() ?: return
        val now = SystemClock.elapsedRealtime()

        if (pkg != lastPkg || now - lastScroll > IDLE_MS) {
            streakStart = now                       // nueva racha
        } else {
            store.addUsage(pkg, now - lastScroll)   // RF-01: tiempo deslizando
        }
        lastPkg = pkg
        lastScroll = now

        if (System.currentTimeMillis() < store.snoozeUntil) return   // pausa de gracia activa
        if (now - streakStart >= store.thresholdSec * 1000L) {
            streakStart = now
            store.interventions = store.interventions + 1
            startActivity(
                Intent(this, FrictionActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra(FrictionActivity.EXTRA_PKG, pkg)
                    .putExtra(FrictionActivity.EXTRA_SECONDS, store.thresholdSec)
            )
        }
    }

    companion object {
        const val IDLE_MS = 3000L
        var instance: ScrollWatcherService? = null
    }
}
