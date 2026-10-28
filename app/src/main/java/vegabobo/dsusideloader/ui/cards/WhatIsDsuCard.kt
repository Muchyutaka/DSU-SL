package vegabobo.dsusideloader.ui.cards

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import vegabobo.dsusideloader.R
import vegabobo.dsusideloader.ui.util.motionDuration

/**
 * Collapsible "What is DSU?" explanation.
 *
 * Collapsed it is a single row with a teaser, expanded it shows a short bullet
 * list and a link to the official documentation. The state is remembered for
 * the session (it survives configuration changes, not process death).
 */
@Composable
fun DsuInfoCard(
    modifier: Modifier = Modifier,
    onOpenDocs: () -> Unit,
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(motionDuration(220)),
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(tween(motionDuration(240))),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(start = 16.dp, end = 4.dp, top = 16.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1F)) {
                    Text(
                        text = stringResource(id = R.string.what_is_dsu),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (!isExpanded) {
                        Text(
                            text = stringResource(id = R.string.what_is_dsu_teaser),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        imageVector = Icons.Outlined.ExpandMore,
                        contentDescription = if (isExpanded) {
                            stringResource(id = R.string.what_is_dsu_collapse)
                        } else {
                            stringResource(id = R.string.what_is_dsu_expand)
                        },
                        modifier = Modifier.rotate(chevronRotation),
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(tween(motionDuration(240))) +
                    fadeIn(tween(motionDuration(200))),
                exit = shrinkVertically(tween(motionDuration(200))) +
                    fadeOut(tween(motionDuration(120))),
            ) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    BulletText(stringResource(id = R.string.what_is_dsu_bullet_what))
                    BulletText(stringResource(id = R.string.what_is_dsu_bullet_how))
                    BulletText(stringResource(id = R.string.what_is_dsu_bullet_risk))
                    BulletText(stringResource(id = R.string.what_is_dsu_bullet_future))
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onOpenDocs) {
                        Icon(
                            imageVector = Icons.Outlined.Language,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = stringResource(id = R.string.what_is_dsu_read_docs))
                    }
                }
            }
        }
    }
}

@Composable
private fun BulletText(text: String) {
    Row(modifier = Modifier.padding(bottom = 6.dp)) {
        Text(
            text = "\u2022",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(end = 8.dp),
        )
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}
