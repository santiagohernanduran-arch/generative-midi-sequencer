# Generative MIDI Sequencer — Android

Aplicación nativa **Android** escrita en **Kotlin** con **Jetpack Compose** que funciona
como un **secuenciador MIDI generativo** de 64 pasos.

```
Android → USB MIDI → Arturia KeyStep Pro → otros instrumentos MIDI
```

Genera secuencias musicales de 64 pasos con reglas orientadas a ambient/minimal y las
envía por **USB MIDI** al Keystep Pro. Funciona 100 % sin conexión a Internet y sin
cuentas de usuario. Todo el procesamiento es local.

---

## Características (V1)

- **Conexión MIDI**: detección automática de dispositivos USB MIDI, botón `REFRESH MIDI`,
  selección de canal MIDI (1–16, por defecto 1).
- **Secuenciador**: 64 pasos, cada paso con nota / silencio / velocity / gate.
  Reproducción continua `1 → 2 → … → 64 → 1` con indicador visual del paso activo.
- **Tempo**: BPM 30–180 (por defecto 62), modificable en reproducción.
- **Escalas**: Chromatic, Major, Minor, Dorian, Minor Pentatonic y **Ryukyu Pentatonic**
  (la principal). Raíz configurable (por defecto **D** – F – G – A – C).
- **Generador**: controles DENSITY, RANDOMNESS, VARIATION, OCTAVE RANGE, NOTE LENGTH.
  `GENERATE` crea los 64 pasos con reglas musicales (no azar caótico):
  repetición de motivos, movimiento conjunto, saltos ocasionales, silencios,
  variaciones de octava controladas.
- **Ambient Mode**: reduce densidad, aumenta silencios, notas más largas, velocity más
  suave, más repetición y menos saltos.
- **Mutate**: modifica solo una pequeña cantidad de pasos según VARIATION
  (baja: 1–3, media: 5–10, alta: 10–20), respetando siempre la escala.
- **Edición manual**: tocar un paso añade/cambia la nota (respetando la escala);
  mantener pulsado elimina la nota.
- **MIDI Clock**: activable `OFF/ON`; envía Clock, Start al PLAY y Stop al STOP.
  El BPM de la app controla el Clock.
- **Seguridad MIDI**: al STOP y al desconectar se envía `Note Off` de todas las notas
  activas. Botón **PANIC** (All Notes Off) para evitar notas colgadas.

---

## Arquitectura

La capa MIDI está separada de la interfaz:

```
app/src/main/java/com/generative/midi/sequencer/
├── MainActivity.kt            → Actividad principal, crea MidiManager + ViewModel.
├── midi/                      → Capa de dominio / MIDI (sin dependencias de UI).
│   ├── Step.kt                → Modelo de paso (nota / silencio / velocity / gate).
│   ├── ScaleManager.kt        → Escalas, notas MIDI, nombres de notas.
│   ├── NoteGenerator.kt       → Reglas musicales de generación de notas.
│   ├── PatternGenerator.kt    → GENERATE y MUTATE de patrones de 64 pasos.
│   ├── ClockManager.kt        → Reloj de alta precisión basado en elapsedRealtime.
│   ├── TransportController.kt → Avance de pasos, Note On/Off, seguridad MIDI.
│   ├── MidiOutput.kt          → API nativa android.media.midi (I/O real).
│   └── MidiManager.kt         → Façade: detección de dispositivos + orquestación.
└── ui/                        → Capa Compose (UI).
    ├── MainViewModel.kt       → Estado de la UI y acciones.
    ├── MainScreen.kt          → Interfaz.
    └── theme/Theme.kt         → Tema oscuro minimalista.
```

El **generador musical** (`PatternGenerator` / `NoteGenerator`) es totalmente
independiente de la UI y de MIDI I/O: recibe parámetros y devuelve listas de `Step`,
por lo que puede modificarse o sustituirse sin tocar la interfaz.

### Temporización

`ClockManager` calcula el intervalo de tick a partir del BPM y usa
`SystemClock.elapsedRealtime()` (esquema *next-tick*) para mantener un timing estable,
independiente de la actualización de la UI. La reproducción continúa aunque la interfaz
se esté redibujando.

---

## Requisitos para compilar

- **JDK 17** (por ejemplo, Temurin 17).
- **Android Gradle Plugin 8.7** y **Gradle 8.10.2** (ya configurados en el proyecto).
- **Android SDK** con `platforms;android-35` y `build-tools` instalados.
- El proyecto incluye el *Gradle wrapper* (`gradlew.bat` / `gradlew`), por lo que no es
  necesario instalar Gradle por separado.

> **Sobre "Android 16"**: la app apunta por defecto a `compileSdk/targetSdk 35`
> (Android 15) por máxima estabilidad de compilación con el AGP 8.7 estable, y funciona
> sin problemas en dispositivos Android 16 (API 36). Si querés orientarla explícitamente
> a API 36, ver la sección *Opcional* más abajo.

---

## Cómo compilar (opción A — Android Studio, recomendada)

1. Instalá **[Android Studio](https://developer.android.com/studio)** (Koala o más reciente).
2. Abrí Android Studio → *Open* → seleccioná la carpeta `GenerativeMidiSequencer`.
3. Dejá que Gradle sincronice el proyecto (descargará las dependencias la primera vez;
   se necesita Internet solo para esta descarga).
4. Conectá el teléfono por USB con la **depuración USB** activada
   (Opciones de desarrollador) o usá un emulador.
5. Pulsá el botón **Run** (▶). Android Studio compilará, instalará y lanzará la app
   automáticamente.

## Cómo compilar (opción B — línea de comandos)

Con el Android SDK instalado y `local.properties` creado (apunta `sdk.dir` a tu SDK),
ejecutá en la carpeta del proyecto:

```powershell
# En Windows (PowerShell):
.\gradlew.bat assembleDebug
```

```bash
# En macOS/Linux:
./gradlew assembleDebug
```

El APK resultante queda en:

```
app/build/outputs/apk/debug/app-debug.apk
```

### Instalar el APK en el teléfono (Android 16)

1. En el teléfono: **Ajustes → Opciones de desarrollador** → activá
   **Depuración USB**.
2. Conectá el teléfono por USB y aceptá el aviso de *permiso de depuración*.
3. Instalá y lanzá:

```powershell
# Verificar que el dispositivo se detecta
adb devices

# Instalar el APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Lanzar la app
adb shell am start -n com.generative.midi.sequencer/.MainActivity
```

> El teléfono debe soportar **USB Host** (requisito declarado en el manifiesto; la gran
> mayoría de teléfonos Android lo cumplen). Para usar MIDI USB se necesita además un
> adaptador **USB-OTG** (USB-C → USB-A) y el cable del Keystep Pro.

---

## Cómo usar

1. Abrí la app.
2. Conectá el Keystep Pro por USB-OTG. Pulsá **REFRESH MIDI** si no se detectó solo.
   Verás `Arturia KeyStep Pro` y `STATUS: CONNECTED`.
3. Elegí **CH** (canal MIDI de salida, canal 1 por defecto).
4. Ajustá **BPM**, **ROOT** (D) y **SCALE** (Ryukyu).
5. Ajustá DENSITY / RANDOMNESS / VARIATION / OCTAVE / NOTE LENGTH, activá
   **AMBIENT MODE** si querés.
6. Pulsá **GENERATE** para crear el patrón, **MUTATE** para variarlo poco a poco.
7. Pulsá **PLAY** para reproducirlo. **STOP** para detener (envía Note Off de todo).
8. **PANIC** detiene cualquier nota colgada al instante (All Notes Off).

---

## Notas sobre MIDI Clock y sincronización

- Con **MIDI CLOCK: ON**, al pulsar PLAY la app envía `Start`, y `Stop` al pulsar STOP.
  El Keystep Pro seguirá el reloj de la app (el BPM de la app manda).
- Con **MIDI CLOCK: OFF**, la app funciona como reloj maestro interno igualmente, pero
  no envía los mensajes de Clock/Start/Stop por el cable. Usá el modo Clock del propio
  Keystep Pro en ese caso.

---

## Opcional: apuntar a Android 16 (API 36)

Para compilar explícitamente contra API 36:

1. Instalá el SDK Platform 36 en Android Studio (SDK Manager).
2. En `app/build.gradle.kts` cambiá:

```kotlin
compileSdk = 36
targetSdk = 36
```

3. En `gradle/libs.versions.toml` actualizá el AGP a una versión estable que soporte
   API 36 (p. ej. `8.9+` o la que esté vigente) y resincronizá.

---

## Versión 2 (próximo paso)

Preparada para **4 pistas MIDI independientes** con el Keystep Pro como centro de
control. La arquitectura ya está desacoplada (`TransportController` por pista,
`PatternGenerator` reutilizable, `Step` por pista) para poder ampliarla sin reescribir
la base.
