package com.ediptan.orgpro

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            OrgProApp()
        }
    }
}

/* ---------------------------------------------------
   SES MOTORU
--------------------------------------------------- */

class OrgSoundEngine {

    private val sampleRate = 44100

    private val tracks = mutableMapOf<Int, AudioTrack>()

    var instrument = "Org"
    var volume = 0.65f
    var transpose = 0

    private fun frequency(midi: Int): Double {
        return 440.0 * 2.0.pow((midi - 69) / 12.0)
    }

    private fun waveform(
        t: Double,
        frequency: Double,
        type: String
    ): Double {

        val phase = 2.0 * PI * frequency * t

        return when (type) {

            "Piyano" ->
                sin(phase) * 0.80 +
                        sin(phase * 2.0) * 0.15 +
                        sin(phase * 3.0) * 0.05

            "Bağlama" ->
                sin(phase) * 0.65 +
                        sin(phase * 2.0) * 0.20 +
                        sin(phase * 3.0) * 0.10 +
                        sin(phase * 5.0) * 0.05

            "Gitar" ->
                sin(phase) * 0.70 +
                        sin(phase * 2.0) * 0.20 +
                        sin(phase * 4.0) * 0.10

            "Flüt" ->
                sin(phase) * 0.92 +
                        sin(phase * 2.0) * 0.06 +
                        sin(phase * 3.0) * 0.02

            else ->
                sin(phase) * 0.55 +
                        sin(phase * 2.0) * 0.25 +
                        sin(phase * 3.0) * 0.12 +
                        sin(phase * 4.0) * 0.08
        }
    }

    fun playNote(originalMidi: Int) {

        val midi = originalMidi + transpose

        stopNote(midi)

        val durationSeconds =
            if (instrument == "Org") 1.2 else 1.5

        val frames = (sampleRate * durationSeconds).toInt()

        val data = ShortArray(frames)

        val frequency = frequency(midi)

        for (i in 0 until frames) {

            val t = i.toDouble() / sampleRate

            val envelope = when {
                t < 0.02 -> t / 0.02
                instrument == "Org" -> 0.92
                else -> (1.0 - t / durationSeconds).coerceAtLeast(0.0)
            }

            val value =
                waveform(t, frequency, instrument) *
                        envelope *
                        volume

            data[i] =
                (value * Short.MAX_VALUE)
                    .toInt()
                    .coerceIn(
                        Short.MIN_VALUE.toInt(),
                        Short.MAX_VALUE.toInt()
                    )
                    .toShort()
        }

        val format = AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val track = AudioTrack.Builder()
            .setAudioAttributes(attributes)
            .setAudioFormat(format)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(data.size * 2)
            .build()

        track.write(data, 0, data.size)

        tracks[midi] = track

        track.play()
    }

    fun stopNote(originalMidi: Int) {

        val midi = originalMidi + transpose

        tracks[midi]?.let {

            try {
                it.stop()
                it.flush()
                it.release()
            } catch (_: Exception) {
            }
        }

        tracks.remove(midi)
    }

    fun release() {

        tracks.values.forEach {

            try {
                it.stop()
                it.release()
            } catch (_: Exception) {
            }
        }

        tracks.clear()
    }
}

/* ---------------------------------------------------
   ANA UYGULAMA
--------------------------------------------------- */

@Composable
fun OrgProApp() {

    val engine = remember {
        OrgSoundEngine()
    }

    var instrument by remember {
        mutableStateOf("Org")
    }

    var octave by remember {
        mutableIntStateOf(4)
    }

    var transpose by remember {
        mutableIntStateOf(0)
    }

    var sustain by remember {
        mutableStateOf(false)
    }

    var volume by remember {
        mutableFloatStateOf(0.65f)
    }

    DisposableEffect(Unit) {

        onDispose {
            engine.release()
        }
    }

    LaunchedEffect(instrument) {
        engine.instrument = instrument
    }

    LaunchedEffect(volume) {
        engine.volume = volume
    }

    LaunchedEffect(transpose) {
        engine.transpose = transpose
    }

    MaterialTheme {

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(12, 12, 15)
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
            ) {

                /* ÜST BAR */

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween,
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Column {

                        Text(
                            "ORG PRO",
                            color = Color.White,
                            fontSize = 25.sp
                        )

                        Text(
                            "Profesyonel Mobil Org",
                            color = Color.LightGray,
                            fontSize = 13.sp
                        )
                    }

                    Row(
                        horizontalArrangement =
                            Arrangement.spacedBy(6.dp)
                    ) {

                        SmallButton(
                            "−",
                            enabled = octave > 1
                        ) {
                            octave--
                        }

                        Text(
                            "Oktav $octave",
                            color = Color.White,
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .padding(horizontal = 6.dp)
                        )

                        SmallButton(
                            "+",
                            enabled = octave < 7
                        ) {
                            octave++
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                /* KONTROLLER */

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(7.dp)
                ) {

                    InstrumentButton(
                        text = instrument,
                        modifier = Modifier.weight(1f)
                    ) {

                        instrument =
                            when (instrument) {
                                "Org" -> "Piyano"
                                "Piyano" -> "Bağlama"
                                "Bağlama" -> "Gitar"
                                "Gitar" -> "Flüt"
                                else -> "Org"
                            }
                    }

                    SmallButton(
                        "T−"
                    ) {

                        transpose =
                            (transpose - 1)
                                .coerceAtLeast(-12)
                    }

                    SmallButton(
                        "T+"
                    ) {

                        transpose =
                            (transpose + 1)
                                .coerceAtMost(12)
                    }

                    SmallButton(
                        if (sustain) "Sustain ✓"
                        else "Sustain"
                    ) {

                        sustain = !sustain
                    }
                }

                Spacer(Modifier.height(7.dp))

                /* SES */

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        "Ses",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )

                    Slider(
                        value = volume,
                        onValueChange = {
                            volume = it
                        },
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        "${(volume * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }

                Spacer(Modifier.height(4.dp))

                /* KLAVYE */

                PianoKeyboard(
                    octave = octave,
                    engine = engine,
                    sustain = sustain
                )
            }
        }
    }
}

/* ---------------------------------------------------
   PİYANO KLAVYESİ
--------------------------------------------------- */

@Composable
fun PianoKeyboard(
    octave: Int,
    engine: OrgSoundEngine,
    sustain: Boolean
) {

    val whiteNotes =
        listOf(
            "C", "D", "E", "F", "G", "A", "B",
            "C", "D", "E", "F", "G", "A", "B"
        )

    val blackPositions =
        listOf(
            0, 1, 3, 4, 5,
            7, 8, 10, 11, 12
        )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(top = 6.dp)
    ) {

        val whiteWidth =
            maxWidth / whiteNotes.size

        val blackWidth =
            whiteWidth * 0.60f

        val baseMidi =
            12 * (octave + 1)

        /* BEYAZ TUŞLAR */

        Row(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(
                    rememberScrollState()
                ),
            horizontalArrangement =
                Arrangement.spacedBy(1.dp)
        ) {

            whiteNotes.forEachIndexed { index, note ->

                val midi =
                    baseMidi + index

                PianoKey(
                    note = note,
                    black = false,
                    modifier = Modifier
                        .width(whiteWidth)
                        .fillMaxHeight()
                ) {

                    engine.playNote(midi)
                }
            }
        }

        /* SİYAH TUŞLAR */

        blackPositions.forEach { position ->

            val midi =
                baseMidi + position + 1

            PianoKey(
                note = "♯",
                black = true,
                modifier = Modifier
                    .width(blackWidth)
                    .height(
                        maxHeight * 0.58f
                    )
                    .offset(
                        x =
                            whiteWidth *
                                    (position + 1) -
                                    blackWidth / 2
                    )
                    .align(Alignment.TopStart)
            ) {

                engine.playNote(midi)
            }
        }
    }
}

/* ---------------------------------------------------
   TUŞ
--------------------------------------------------- */

@Composable
fun PianoKey(
    note: String,
    black: Boolean,
    modifier: Modifier,
    onPressed: () -> Unit
) {

    var pressed by remember {
        mutableStateOf(false)
    }

    val background =
        if (black) {

            if (pressed)
                Color(80, 80, 90)
            else
                Color(18, 18, 20)

        } else {

            if (pressed)
                Color(210, 210, 215)
            else
                Color(245, 245, 242)
        }

    Box(
        modifier = modifier
            .background(
                background,
                RoundedCornerShape(
                    bottomStart = 7.dp,
                    bottomEnd = 7.dp
                )
            )
            .pointerInput(Unit) {

                awaitEachGesture {

                    awaitFirstDown()

                    pressed = true

                    onPressed()

                    awaitPointerEventScope {

                        do {

                            val event =
                                awaitPointerEvent()

                            if (
                                event.changes
                                    .all {
                                        !it.pressed
                                    }
                            ) {
                                break
                            }

                        } while (true)
                    }

                    pressed = false
                }
            },
        contentAlignment =
            Alignment.BottomCenter
    ) {

        if (!black) {

            Text(
                text = note,
                color = Color.DarkGray,
                fontSize = 11.sp,
                modifier = Modifier.padding(
                    bottom = 12.dp
                )
            )
        }
    }
}

/* ---------------------------------------------------
   BUTONLAR
--------------------------------------------------- */

@Composable
fun SmallButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {

    Button(
        onClick = onClick,
        enabled = enabled,
        contentPadding =
            PaddingValues(
                horizontal = 10.dp
            ),
        shape = RoundedCornerShape(9.dp)
    ) {

        Text(text)
    }
}

@Composable
fun InstrumentButton(
    text: String,
    modifier: Modifier,
    onClick: () -> Unit
) {

    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(9.dp)
    ) {

        Text(text)
    }
}
