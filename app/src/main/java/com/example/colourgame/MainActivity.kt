package com.example.colourgame

import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.ui.layout.ContentScale
import com.example.colourgame.ui.theme.ColourgameTheme
import kotlinx.coroutines.delay
import com.airbnb.lottie.compose.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp

class MainActivity : ComponentActivity() {
    private var timerPlayer: MediaPlayer? = null
    private var correctPlayer: MediaPlayer? = null
    private var wrongPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var spokenText by remember { mutableStateOf("") }
            var errorMessage by remember { mutableStateOf<String?>(null) }
            var listening by remember { mutableStateOf(false) }
            var score by remember { mutableStateOf(0) }
            var currentIndex by remember { mutableStateOf(0) }
            var round by remember { mutableStateOf(1) }
            var timeLeft by remember { mutableStateOf(30) }
            val maxRounds = 10
            val colors = listOf(
                "Red" to Color.Red,
                "Green" to Color.Green,
                "Blue" to Color.Blue,
                "Yellow" to Color.Yellow,
                "Magenta" to Color.Magenta,
                "Cyan" to Color.Cyan,
                "Black" to Color.Black,
                "White" to Color.White,
                "Gray" to Color.Gray,
                "Orange" to Color(0xFFFFA500)
            )
            val usedIndices = remember { mutableStateListOf<Int>() }
            val showScoreCard = remember { mutableStateOf(false) }

            DisposableEffect(Unit) {
                timerPlayer = MediaPlayer.create(this@MainActivity, R.raw.timeout)
                correctPlayer = MediaPlayer.create(this@MainActivity, R.raw.correct)
                wrongPlayer = MediaPlayer.create(this@MainActivity, R.raw.wrong)
                onDispose {
                    timerPlayer?.release()
                    correctPlayer?.release()
                    wrongPlayer?.release()
                }
            }

            LaunchedEffect(Unit) {
                if (usedIndices.isEmpty()) {
                    val firstIndex = (colors.indices).random()
                    usedIndices.add(firstIndex)
                    currentIndex = firstIndex
                }
            }

            val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    val data = result.data
                    val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    spokenText = matches?.firstOrNull()?.trim() ?: ""
                    val expected = colors[currentIndex].first.lowercase().trim()
                    val heard = spokenText.lowercase().trim()
                    if (heard == expected) {
                        score++
                        errorMessage = "Correct!"
                        correctPlayer?.start()
                    } else {
                        errorMessage = "Wrong!"
                        wrongPlayer?.start()
                    }
                    listening = false
                    timerPlayer?.pause()
                    timerPlayer?.seekTo(0)
                    if (round < maxRounds) {
                        round++
                        var nextIndex: Int
                        do {
                            nextIndex = (colors.indices).random()
                        } while (usedIndices.contains(nextIndex) && usedIndices.size < colors.size)
                        usedIndices.add(nextIndex)
                        currentIndex = nextIndex
                        timeLeft = 30
                    } else {
                        showScoreCard.value = true
                        listening = false
                    }
                } else {
                    errorMessage = "Speech cancelled or failed."
                    listening = false
                    timerPlayer?.pause()
                    timerPlayer?.seekTo(0)
                }
            }

            fun launchGoogleSpeech() {
                errorMessage = null
                spokenText = ""
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Say the colour name")
                }
                listening = true
                speechLauncher.launch(intent)
            }

            // Timer logic: only runs when listening is true
            LaunchedEffect(listening, round) {
                if (listening && !showScoreCard.value) {
                    timeLeft = 30
                    timerPlayer?.isLooping = true
                    timerPlayer?.start()
                    while (timeLeft > 0 && listening) {
                        delay(1000)
                        timeLeft--
                    }
                    timerPlayer?.pause()
                    timerPlayer?.seekTo(0)
                    if (timeLeft == 0 && listening) {
                        errorMessage = "Time's up!"
                        wrongPlayer?.start()
                        listening = false
                        if (round < maxRounds) {
                            round++
                            var nextIndex: Int
                            do {
                                nextIndex = (colors.indices).random()
                            } while (usedIndices.contains(nextIndex) && usedIndices.size < colors.size)
                            usedIndices.add(nextIndex)
                            currentIndex = nextIndex
                            timeLeft = 30
                        } else {
                            showScoreCard.value = true
                        }
                    }
                }
            }

            ColourGameApp(
                spokenText = spokenText,
                errorMessage = errorMessage,
                listening = listening,
                onSpeak = { launchGoogleSpeech() },
                score = score,
                currentIndex = currentIndex,
                round = round,
                timeLeft = timeLeft,
                maxRounds = maxRounds,
                colors = colors,
                onRestart = {
                    spokenText = ""
                    errorMessage = null
                    listening = false
                    score = 0
                    usedIndices.clear()
                    val firstIndex = (colors.indices).random()
                    usedIndices.add(firstIndex)
                    currentIndex = firstIndex
                    round = 1
                    timeLeft = 30
                    showScoreCard.value = false
                },
                showScoreCard = showScoreCard.value
            )
        }
    }
}

@Composable
fun CountdownTimer(
    timeLeft: Int,
    totalTime: Int,
    modifier: Modifier = Modifier,
    color: Color = Color.Red,
    backgroundColor: Color = Color.LightGray,
    strokeWidth: Dp = 12.dp
) {
    val progress = timeLeft / totalTime.toFloat()
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000),
        label = "timerAnim"
    )

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Canvas(modifier = Modifier.size(120.dp)) {
            val sweep = 360 * animatedProgress
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            // Background circle
            drawArc(
                color = backgroundColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke
            )
            // Foreground arc
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                style = stroke
            )
        }
        Text(
            text = "$timeLeft s",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )
    }
}

@Composable
fun ColourGameApp(
    spokenText: String,
    errorMessage: String?,
    listening: Boolean,
    onSpeak: () -> Unit,
    score: Int,
    currentIndex: Int,
    round: Int,
    timeLeft: Int,
    maxRounds: Int,
    colors: List<Pair<String, Color>>,
    onRestart: () -> Unit,
    showScoreCard: Boolean
) {
    ColourgameTheme {
        if (showScoreCard) {
            val infiniteTransition = rememberInfiniteTransition(label = "scoreAnim")
            val animatedDark by infiniteTransition.animateColor(
                initialValue = Color(0xFF22223B),
                targetValue = Color(0xFF4A4E69),
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ), label = "scoreBg"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(animatedDark),
                contentAlignment = Alignment.Center
            ) {
                val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.score_bg))
                LottieAnimation(
                    composition = composition,
                    iterations = LottieConstants.IterateForever,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Game Over!",
                        style = MaterialTheme.typography.displayLarge,
                        color = Color.White,
                        modifier = Modifier.padding(16.dp)
                    )
                    Text(
                        text = "Your Score: $score",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color(0xFFFFC300),
                        modifier = Modifier.padding(16.dp)
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(onClick = onRestart) {
                        Text("Start Again")
                    }
                }
            }
        } else {
            Scaffold(modifier = Modifier.fillMaxSize()) { contentPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding),
                    contentAlignment = Alignment.Center
                ) {
                    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.back_bg))
                    LottieAnimation(
                        composition = composition,
                        iterations = LottieConstants.IterateForever,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = colors[currentIndex].first,
                            style = MaterialTheme.typography.displayLarge,
                            color = colors[currentIndex].second,
                            modifier = Modifier.padding(32.dp)
                        )
                        Text(
                            text = "Score: $score",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )
                        Text(
                            text = "Round: $round/$maxRounds",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White
                        )
                        CountdownTimer(
                            timeLeft = timeLeft,
                            totalTime = 30,
                            modifier = Modifier.padding(16.dp)
                        )
                        if (spokenText.isNotEmpty()) {
                            Text(
                                text = "You said: $spokenText",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White
                            )
                        }
                        if (errorMessage != null) {
                            Text(
                                text = errorMessage,
                                color = if (errorMessage == "Correct!") Color.Green else Color.Red,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = onSpeak,
                            enabled = !listening,
                            modifier = Modifier
                                .defaultMinSize(minWidth = 100.dp)
                                .height(40.dp)
                        ) {
                            Text("Speak Colour")
                        }
                    }
                }
            }
        }
    }
}