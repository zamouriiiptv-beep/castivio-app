package com.castivio.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import com.castivio.core.design.components.CastivioButton
import com.castivio.core.design.components.CastivioIconButton
import com.castivio.core.design.components.castivioBodyStyle
import com.castivio.core.design.components.castivioChipStyle
import com.castivio.core.design.components.castivioTitleStyle
import com.castivio.core.design.theme.CastivioMetrics
import com.castivio.core.design.theme.CastivioTheme
import com.castivio.core.design.theme.Radius
import com.castivio.core.design.theme.Spacing
import com.castivio.core.design.theme.castivioBackdrop
import com.castivio.core.design.theme.castivioStage
import com.castivio.core.platform.Smoothness
import com.castivio.core.platform.SmoothnessBook

/**
 * The smoothness panel (design/mockups/smoothness-panel.html): every screen the application has shown since the panel was
 * last cleared, and every action timed on its own -- how long each took to come, and how the main thread kept up while it
 * was on display. Development and measurement builds only, in English like the strips, left to right whatever the
 * application's language: a table of figures read by whoever is measuring.
 *
 * A screen whose frames came late more than [LATE_WARN_PERCENT] percent of the time, or one that took longer than
 * [OPEN_WARN_MS] to come, is drawn in the warning hue: the budgets of PERFORMANCE.md are a frame in 16.6 ms and a usable
 * Home in 400 ms.
 */
@Composable
internal fun SmoothnessPanel(m: CastivioMetrics, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val rows by Smoothness.rows.collectAsState()
    val colors = CastivioTheme.colors
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            Modifier
                .fillMaxSize()
                .castivioBackdrop()
                .pointerInput(Unit) {}
                .castivioStage(m)
                .verticalScroll(rememberScrollState())
                .testTag(SMOOTHNESS_PANEL_TAG),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                CastivioIconButton(icon = Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", onClick = onClose)
                Text(
                    SMOOTHNESS_TITLE,
                    style = castivioTitleStyle(m.fsTitle),
                    color = colors.onBackground,
                    modifier = Modifier.weight(1f),
                )
                CastivioButton(text = "Clear", onClick = { Smoothness.reset() }, icon = Icons.Rounded.Refresh)
            }
            Text(
                "opens: median/slowest ms to the first frame · frames: main-thread frames on screen, late = more than 25 ms " +
                    "after the one before · budgets: 16.6 ms a frame, Home in 400 ms",
                style = castivioChipStyle(m.fsChip),
                color = colors.onBackgroundMuted,
            )
            if (rows.isEmpty()) {
                Text("Nothing measured yet: use the application, then come back.", style = castivioBodyStyle(m.fsBody), color = colors.onBackgroundVariant)
            }
            rows.forEach { row -> SmoothnessLine(row, m) }
        }
    }
}

@Composable
private fun SmoothnessLine(row: SmoothnessBook.Row, m: CastivioMetrics) {
    val colors = CastivioTheme.colors
    val shape = RoundedCornerShape(Radius.sm)
    val slow = (row.openMedianMs ?: 0) > OPEN_WARN_MS || (row.latePercent ?: 0.0) > LATE_WARN_PERCENT
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.glassFill)
            .border(1.dp, if (slow) colors.warning else colors.glassBorderSoft, shape)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
    ) {
        Text(
            row.name,
            style = castivioBodyStyle(m.fsBody).copy(fontWeight = FontWeight.SemiBold, textDirection = TextDirection.Ltr),
            color = if (slow) colors.warning else colors.onBackground,
        )
        Text(
            row.describe().removePrefix(row.name).removePrefix(" · "),
            style = castivioChipStyle(m.fsChip).copy(textDirection = TextDirection.Ltr),
            color = colors.onBackgroundVariant,
        )
    }
}

/** The panel's scrolling column, for a test to find. */
internal const val SMOOTHNESS_PANEL_TAG = "castivio.settings.smoothness"

internal const val SMOOTHNESS_TITLE = "Smoothness"
internal const val SMOOTHNESS_ROW = "Smoothness panel"

/** A screen slower than this, at the median, is drawn in the warning hue: Home's budget. */
private const val OPEN_WARN_MS = 400L

/** A screen whose frames came late more often than this, in percent, is drawn in the warning hue. */
private const val LATE_WARN_PERCENT = 5.0
