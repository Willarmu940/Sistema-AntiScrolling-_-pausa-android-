# Pausa — app móvil para reducir el scroll infinito (avance)

Proyecto de Formulación y evaluación de proyectos, Universidad Central.
Prototipo Android (Kotlin, minSdk 26) que monitorea el uso de apps con scroll infinito,
detecta racha de deslizamiento continuo e interviene con una pantalla de fricción.

## Cómo ejecutarlo
1. Abrir la carpeta en Android Studio (Koala o superior) y esperar el sync de Gradle.
2. Conectar un celular Android (depuración USB) y ejecutar el módulo `app`.
3. En la app: tocar **Activar detección de scroll** y habilitar *Pausa* en Accesibilidad;
   tocar **Dar acceso al tiempo de uso** y habilitarlo.
4. Tocar **Abrir feed de demo** y deslizar sin parar ~20 s (umbral ajustable en la pantalla principal).
   Aparece la pantalla de fricción. También funciona en TikTok, Instagram, YouTube, X y Reddit.

## Estado por requerimiento
| Requerimiento | Estado | Dónde |
|---|---|---|
| RF-01 Monitoreo de uso | Parcial | `MainActivity` (UsageStats) + `ScrollWatcherService` |
| RF-02 Límites personalizados | Parcial (solo umbral global) | `MainActivity`, `Store` |
| RF-03 Alerta por scroll continuo | Completa (versión demo) | `ScrollWatcherService` |
| RF-04 Pantalla de fricción | Completa | `FrictionActivity` |
| RF-05 Pausas guiadas | Pendiente (solo respiración) | — |
| RF-06 Recompensas | Parcial (puntos) | `FrictionActivity`, `Store` |
| RF-07 Restringir omitir límites | Parcial (espera obligatoria) | `FrictionActivity` |
| RF-08 Estadísticas | Parcial (resumen diario) | `MainActivity` |
| RF-09 Almacenamiento básico | Completa | `Store` (SharedPreferences) |

## Limitaciones conocidas
- Solo Android. iOS restringe estas capacidades (Screen Time API requiere entitlement de Apple).
- El umbral es de segundos para la demo; la meta del documento es 20 minutos.
- Los datos viven solo en el dispositivo (privacidad, RNF-04).

## Compilar sin Android Studio
Cada `push` a `main` compila el APK en GitHub Actions (`.github/workflows/build.yml`).
Descárgalo desde la pestaña **Actions → última ejecución → Artifacts → pausa-debug-apk**
e instálalo en el celular.
