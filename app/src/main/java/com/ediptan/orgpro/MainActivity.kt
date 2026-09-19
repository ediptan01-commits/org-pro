package com.ediptan.orgpro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            OrgProApp()
        }
    }
}

@Composable
fun OrgProApp() {
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(18, 18, 18))
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "ORG PRO",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium
            )

            Text(
                text = "Profesyonel Mobil Org",
                color = Color.LightGray
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = {}) {
                    Text("Piyano")
                }

                Button(onClick = {}) {
                    Text("Ritim")
                }

                Button(onClick = {}) {
                    Text("Ayarlar")
                }
            }

            Text(
                text = "KLAVYE",
                color = Color.White,
                modifier = Modifier.padding(8.dp)
            )

            PianoKeyboard()
        }
    }
}

@Composable
fun PianoKeyboard() {

    val notes = listOf(
        "C", "D", "E", "F", "G", "A", "B",
        "C", "D", "E", "F", "G", "A", "B"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {

        notes.forEach { note ->

            Button(
                onClick = {
                    // Bir sonraki aşamada gerçek ses burada çalacak.
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                shape = RoundedCornerShape(
                    bottomStart = 8.dp,
                    bottomEnd = 8.dp
                )
            ) {
                Text(
                    text = note,
                    color = Color.Black
                )
            }
        }
    }
}
