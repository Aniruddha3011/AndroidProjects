package com.example.dicerollerapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Animatable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dicerollerapp.Presentation.DiceRollerScreen
import com.example.dicerollerapp.ui.theme.DiceRollerAppTheme
import kotlinx.coroutines.launch
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DiceRollerAppTheme {
                DiceRollerApp()
            }
        }
    }
}

@Composable
fun DiceRollerApp(modifier: Modifier = Modifier) {
    var dicevalue by remember{ mutableStateOf(1) }

    var isRollling by remember{ mutableIntStateOf(0) }

    val rotation=remember { Animatable(0f) }

    val scale=remember { Animatable(1f) }

    val coroutineScope= rememberCoroutineScope()


    Surface(
        modifier= Modifier.fillMaxSize(),
        color= MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier=Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ){
            Text(
                text="Dice Roller",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color= MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(48.dp))

            Image(
                painter = painterResource(getDicedrawable(dicevalue)),
                contentDescription = "Dice showing $dicevalue",
                modifier=Modifier
                    .size(200.dp)
                    .graphicsLayer(
                        rotationZ = rotation.value,
                        scaleX = scale.value,
                        scaleY = scale.value
                    )
            )
            Spacer(modifier=Modifier.height(48.dp))

            Button(
                onClick = {
                    coroutineScope.launch {
                        isRollling++

                        // Animation Rotation

                        rotation.animateTo(
                            targetValue = rotation.value+360f,
                            animationSpec = tween(
                                durationMillis = 500,
                                easing =FastOutSlowInEasing
                            )
                        )

                        // Scale animation

                        scale.animateTo(
                            targetValue = 1.2f,
                            animationSpec = tween(durationMillis = 100)
                        )

                        scale.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(
                                durationMillis = 100
                            )
                        )

                        // generte new dice value

                        dicevalue= Random.nextInt(1,7)

                    }
                },
                modifier=Modifier.size(width=200.dp,height=56.dp),
                colors= ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text="Flip",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(48.dp))



        }

    }
}

@Composable
fun getDicedrawable(value:Int):Int {
    return when(value){
        1-> R.drawable.dice1
        2-> R.drawable.dice2
        3-> R.drawable.dice3
        4-> R.drawable.dice4
        5-> R.drawable.dice5
        else -> R.drawable.dice6

    }
}

@Composable
@Preview(showSystemUi = true)
fun previewdice(modifier: Modifier = Modifier) {
    DiceRollerApp()
}