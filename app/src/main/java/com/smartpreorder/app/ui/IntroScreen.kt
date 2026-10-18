package com.smartpreorder.app.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.smartpreorder.app.R
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private data class IntroPage(
    @DrawableRes val image: Int,
    val title: String,
    val subtitle: String
)

/** First-launch app intro. Shown once; [onFinish] is called on Skip / Get Started. */
@Composable
fun IntroScreen(onFinish: () -> Unit) {
    val pages = remember {
        listOf(
            IntroPage(R.drawable.ill_chef, "Delicious Food,\nPre-Ordered", "Browse menus from your favourite restaurants and order before you arrive."),
            IntroPage(R.drawable.ill_store, "Choose from the\nBest Restaurants", "Discover top-rated places near you and pick exactly what you crave."),
            IntroPage(R.drawable.ill_clock, "Pre-Order &\nSave Time", "Skip the queue. Your meal is ready right when you walk in."),
            IntroPage(R.drawable.ill_rider, "Enjoy Your Food\nAt Your Convenience", "Track your order live and pick up or dine in, hassle free."),
        )
    }
    val pager = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLast = pager.currentPage == pages.lastIndex

    val float = rememberInfiniteTransition(label = "float")
    val bob = float.animateFloat(
        initialValue = -8f, targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob"
    )

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF241208), BrandDark, BrandDark))
        )
    ) {
        // soft orange glow behind the illustration
        Box(
            Modifier.align(Alignment.TopCenter).offset(y = 110.dp).size(420.dp)
                .background(
                    Brush.radialGradient(listOf(BrandOrange.copy(alpha = 0.30f), Color.Transparent)),
                    CircleShape
                )
        )

        Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 24.dp)) {
            Row(
                Modifier.fillMaxWidth().height(56.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.app_icon), null, Modifier.size(40.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = Color.White)) { append("Smart") }
                            withStyle(SpanStyle(color = BrandOrange)) { append("PreOrder") }
                        },
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold
                    )
                }
                if (!isLast) {
                    TextButton(onClick = onFinish) {
                        Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                    }
                }
            }

            HorizontalPager(state = pager, modifier = Modifier.weight(1f).fillMaxWidth()) { page ->
                val dist = ((pager.currentPage - page) + pager.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painterResource(pages[page].image), null,
                        Modifier.fillMaxWidth().height(300.dp).graphicsLayer {
                            val s = 1f - dist * 0.25f
                            scaleX = s; scaleY = s
                            alpha = 1f - dist * 0.7f
                            translationY = bob.value * density
                        },
                        contentScale = ContentScale.Fit
                    )
                    Spacer(Modifier.height(28.dp))
                    Column(
                        Modifier.graphicsLayer { alpha = 1f - dist * 0.9f },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            pages[page].title, textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            pages[page].subtitle, textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
            }

            // page indicators
            Row(
                Modifier.fillMaxWidth().padding(vertical = 20.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pages.size) { i ->
                    val selected = pager.currentPage == i
                    val w = animateDpAsState(if (selected) 28.dp else 8.dp, tween(300), label = "dot")
                    Box(
                        Modifier.padding(horizontal = 4.dp).height(8.dp).width(w.value)
                            .background(
                                if (selected) BrandOrange else Color(0xFF3A4552),
                                RoundedCornerShape(50)
                            )
                    )
                }
            }

            Button(
                onClick = {
                    if (isLast) onFinish()
                    else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                },
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandOrange, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(if (isLast) "Get Started" else "Next", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
