package com.ediptan.orgpro

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

/* =========================================================
   SES MOTORU
   ========================================================= */

class OrgSoundEngine {

    private val sampleRate = 44100

    private val tracks =
        mutableMapOf<Int, AudioTrack>()

    var instrument: String = "Org"
    var volume: Float = 0.65f
    var transpose: Int = 0

    private fun midiToFrequency(midi: Int): Double {
        return 440.0 * 2.0.pow(
            (midi - 69) / 12.0
        )
    }

    private fun waveform(
        time: Double,
        frequency: Double,
        type: String
    ): Double {

        val phase =
            2.0 * PI * frequency * time

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

        val midi =
            originalMidi + transpose

        stopNote(midi)

        val duration =
            if (instrument == "Org") {
                2.0
            } else {
                1.5
            }

        val frameCount =
            (sampleRate * duration).toInt()

        val audioData =
            ShortArray(frameCount)

        val frequency =
            midiToFrequency(midi)

        for (i in 0 until frameCount) {

            val time =
                i.toDouble() / sampleRate

            val envelope =
                when {

                    time < 0.02 ->
                        time / 0.02

                    instrument == "Org" ->
                        0.92

                    else ->
                        (
                            1.0 -
                                    time / duration
                            ).coerceAtLeast(0.0)
                }

            val sample =
                waveform(
                    time,
                    frequency,
                    instrument
                ) *
                        envelope *
                        volume

            audioData[i] =
                (
                    sample * Short.MAX_VALUE
                )
                    .toInt()
                    .coerceIn(
                        Short.MIN_VALUE.toInt(),
                        Short.MAX_VALUE.toInt()
                    )
                    .toShort()
        }

        val format =
            AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(
                    AudioFormat.ENCODING_PCM_16BIT
                )
                .setChannelMask(
                    AudioFormat.CHANNEL_OUT_MONO
                )
                .build()

        val attributes =
            AudioAttributes.Builder()
                .setUsage(
                    AudioAttributes.USAGE_MEDIA
                )
                .setContentType(
                    AudioAttributes.CONTENT_TYPE_MUSIC
                )
                .build()

        val track =
            AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(format)
                .setTransferMode(
                    AudioTrack.MODE_STATIC
                )
                .setBufferSizeInBytes(
                    audioData.size * 2
                )
                .build()

        track.write(
            audioData,
            0,
            audioData.size
        )

        tracks[midi] = track

        track.play()
    }

    fun stopNote(originalMidi: Int) {

        val midi =
            originalMidi + transpose

        val track =
            tracks[midi]

        if (track != null) {

            try {
                track.stop()
            } catch (_: Exception) {
            }

            try {
                track.flush()
            } catch (_: Exception) {
            }

            try {
                track.release()
            } catch (_: Exception) {
            }
        }

        tracks.remove(midi)
    }

    fun release() {

        tracks.values.forEach { track ->

            try {
                track.stop()
            } catch (_: Exception) {
            }

            try {
                track.release()
            } catch (_: Exception) {
            }
        }

        tracks.clear()
    }
}

/* =========================================================
   ANA UYGULAMA
   ========================================================= */

@Composable
fun OrgProApp() {

    val engine =
        remember {
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

                /* ============================
                   ÜST BAR
                   ============================ */

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween,
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Column {

                        Text(
                            text = "ORG PRO",
                            color = Color.White,
                            fontSize = 25.sp
                        )

                        Text(
                            text = "Profesyonel Mobil Org",
                            color = Color.LightGray,
                            fontSize = 13.sp
                        )
                    }

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically,
                        horizontalArrangement =
                            Arrangement.spacedBy(5.dp)
                    ) {

                        SmallButton(
                            text = "−",
                            enabled = octave > 1
                        ) {
                            octave--
                        }

                        Text(
                            text = "Oktav $octave",
                            color = Color.White,
                            fontSize = 13.sp
                        )

                        SmallButton(
                            text = "+",
                            enabled = octave < 7
                        ) {
                            octave++
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.height(7.dp)
                )

                /* ============================
                   KONTROLLER
                   ============================ */

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(6.dp)
                ) {

                    InstrumentButton(
                        text = instrument,
                        modifier = Modifier.weight(1f)
                    ) {

                        instrument =
                            when (instrument) {

                                "Org" ->
                                    "Piyano"

                                "Piyano" ->
                                    "Bağlama"

                                "Bağlama" ->
                                    "Gitar"

                                "Gitar" ->
                                    "Flüt"

                                else ->
                                    "Org"
                            }
                    }

                    SmallButton(
                        text = "T−"
                    ) {

                        transpose =
                            (transpose - 1)
                                .coerceAtLeast(-12)
                    }

                    SmallButton(
                        text = "T+"
                    ) {

                        transpose =
                            (transpose + 1)
                                .coerceAtMost(12)
                    }

                    SmallButton(
                        text =
                            if (sustain)
                                "Sustain ✓"
                            else
                                "Sustain"
                    ) {

                        sustain = !sustain
                    }
                }

                Spacer(
                    modifier = Modifier.height(5.dp)
                )

                /* ============================
                   SES
                   ============================ */

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text = "Ses",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )

                    Slider(
                        value = volume,
                        onValueChange = {
                            volume = it
                        },
                        modifier =
                            Modifier.weight(1f)
                    )

                    Text(
                        text =
                            "${(volume * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }

                Spacer(
                    modifier = Modifier.height(3.dp)
                )

                /* ============================
                   KLAVYE
                   ============================ */

                PianoKeyboard(
                    octave = octave,
                    engine = engine
                )
            }
        }
    }
}

/* =========================================================
   PİYANO KLAVYESİ
   ========================================================= */

@Composable
fun PianoKeyboard(
    octave: Int,
    engine: OrgSoundEngine
) {

    val whiteNotes =
        listOf(
            "C",
            "D",
            "E",
            "F",
            "G",
            "A",
            "B",
            "C",
            "D",
            "E",
            "F",
            "G",
            "A",
            "B"
        )

    val blackPositions =
        listOf(
            0,
            1,
            3,
            4,
            5,
            7,
            8,
            10,
            11,
            12
        )

    val baseMidi =
        12 * (octave + 1)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(270.dp)
            .padding(top = 5.dp)
    ) {

        val whiteWidth =
            maxWidth / whiteNotes.size

        val blackWidth =
            whiteWidth * 0.60f

        /* ============================
           BEYAZ TUŞLAR
           ============================ */

        Row(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(
                    rememberScrollState()
                ),
            horizontalArrangement =
                Arrangement.spacedBy(1.dp)
        ) {

            whiteNotes.forEachIndexed {
                    index,
                    note ->

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

        /* ============================
           SİYAH TUŞLAR
           ============================ */

        blackPositions.forEach { position ->

            val midi =
                baseMidi +
                        position +
                        1

            PianoKey(
                note = "♯",
                black = true,
                modifier = Modifier
                    .width(blackWidth)
                    .height(155.dp)
                    .offset(
                        x =
                            whiteWidth *
                                    (position + 1) -
                                    blackWidth / 2
                    )
                    .align(
                        Alignment.TopStart
                    )
            ) {

                engine.playNote(midi)
            }
        }
    }
}

/* =========================================================
   PİYANO TUŞU
   ========================================================= */

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

    val keyColor =
        if (black) {

            if (pressed) {
                Color(75, 75, 82)
            } else {
                Color(18, 18, 20)
            }

        } else {

            if (pressed) {
                Color(205, 205, 210)
            } else {
                Color(245, 245, 242)
            }
        }

    Box(
        modifier = modifier
            .background(
                color = keyColor,
                shape = RoundedCornerShape(
                    bottomStart = 7.dp,
                    bottomEnd = 7.dp
                )
            )
            .pointerInput(note) {

                detectTapGestures(

                    onPress = {

                        pressed = true

                        onPressed()

                        try {
                            awaitRelease()
                        } finally {
                            pressed = false
                        }
                    }
                )
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
                    bottom = 10.dp
                )
            )
        }
    }
}

/* =========================================================
   KÜÇÜK BUTON
   ========================================================= */

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
                horizontal = 9.dp,
                vertical = 2.dp
            ),
        shape =
            RoundedCornerShape(9.dp)
    ) {

        Text(
            text = text,
            fontSize = 12.sp
        )
    }
}

/* =========================================================
   ENSTRÜMAN BUTONU
   ========================================================= */

@Composable
fun InstrumentButton(
    text: String,
    modifier: Modifier,
    onClick: () -> Unit
) {

    Button(
        onClick = onClick,
        modifier = modifier,
        contentPadding =
            PaddingValues(
                horizontal = 8.dp,
                vertical = 2.dp
            ),
        shape =
            RoundedCornerShape(9.dp)
    ) {

        Text(
            text = text,
            fontSize = 12.sp
        )
    }
}
