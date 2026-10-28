package vegabobo.dsusideloader.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

/**
 * Title of a card, using the card title token of the app
 * ([MaterialTheme.typography.titleMedium] with a semibold weight).
 */
@Composable
fun CardTitle(modifier: Modifier = Modifier, cardTitle: String) {
    Text(
        modifier = modifier,
        text = cardTitle,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Start,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}
