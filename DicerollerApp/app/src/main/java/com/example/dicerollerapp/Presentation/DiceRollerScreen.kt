package com.example.dicerollerapp.Presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dicerollerapp.R
import kotlin.random.Random

@Composable
fun DiceRollerScreen(modifier: Modifier = Modifier) {
    var click by remember{ mutableStateOf(R.drawable.dice1) }

    val images= listOf(
        R.drawable.dice1,
        R.drawable.dice2,
        R.drawable.dice3,
        R.drawable.dice4,
        R.drawable.dice5,
        R.drawable.dice6
    )

    Column(
       modifier=Modifier
           .fillMaxSize()
           .padding(5.dp)  ,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(click),
            contentDescription = null,
            modifier = Modifier.size(145.dp)
        )

        Button(
            onClick = {
              click=images.random()
            },
            modifier=Modifier.fillMaxWidth().background(color= Color.Black),
        ) {
            Text(
                text = "flip"
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun previewDiceroller(modifier: Modifier = Modifier) {
    DiceRollerScreen()
}