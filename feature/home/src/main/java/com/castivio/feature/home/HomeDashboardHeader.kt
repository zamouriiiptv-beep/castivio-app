package com.castivio.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.castivio.core.design.components.CastivioClock
import com.castivio.core.design.components.CastivioLockup
import com.castivio.core.design.components.InteractiveGlassCard
import com.castivio.core.design.components.castivioChipStyle
import com.castivio.core.design.theme.CastivioMetrics
import com.castivio.core.design.theme.CastivioTheme
import com.castivio.core.design.theme.LocalThemeSwitch

/**
 * Extracted Home header.
 *
 * wordSize is supplied by HomeScreen so this file does not depend on a private
 * typography ratio declared elsewhere in HomeScreen.kt.
 */
@Composable
internal fun HomeDashboardHeader(
    frame: CastivioMetrics,
    height: Dp,
    wordSize: TextUnit,
    onRefresh: () -> Unit,
    onChangeList: () -> Unit,
    onCatchUp: () -> Unit,
    onSettings: () -> Unit,
    onLanguage: () -> Unit,
    onExit: () -> Unit,
) {
    val themeSwitch = LocalThemeSwitch.current
    val colors = CastivioTheme.colors

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // 1. اليسار: الشعار + خط فاصل + زر الخروج
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    CastivioLockup(
                        markSize = frame.brand,
                        wordSize = wordSize,
                    )
                    HomeHeaderDivider()
                    HomeHeaderAction(
                        icon = Icons.Rounded.PowerSettingsNew,
                        label = stringResource(R.string.home_exit),
                        frame = frame,
                        onClick = onExit,
                    )
                }

                // 2. الوسط: الأزرار الستة الموحدة
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    HomeHeaderAction(
                        icon = Icons.Rounded.Refresh,
                        label = stringResource(R.string.home_refresh),
                        frame = frame,
                        onClick = onRefresh,
                    )
                    HomeHeaderAction(
                        icon = Icons.Rounded.SwapHoriz,
                        label = stringResource(R.string.home_change),
                        frame = frame,
                        onClick = onChangeList,
                    )
                    HomeHeaderAction(
                        icon = Icons.Rounded.History,
                        label = stringResource(R.string.catchup_title),
                        frame = frame,
                        onClick = onCatchUp,
                    )
                    HomeHeaderAction(
                        icon = Icons.Rounded.Settings,
                        label = stringResource(R.string.home_settings),
                        frame = frame,
                        onClick = onSettings,
                    )
                    HomeHeaderAction(
                        icon = Icons.Rounded.Language,
                        label = stringResource(R.string.home_language),
                        frame = frame,
                        onClick = onLanguage,
                    )
                    HomeHeaderAction(
                        icon = if (themeSwitch?.isDark == true) {
                            Icons.Rounded.LightMode
                        } else {
                            Icons.Rounded.DarkMode
                        },
                        label = stringResource(
                            if (themeSwitch?.isDark == true) {
                                R.string.home_theme_lighter
                            } else {
                                R.string.home_theme_darker
                            },
                        ),
                        frame = frame,
                        onClick = { themeSwitch?.toggle() },
                    )
                }

                // 3. اليمين: خط فاصل + الساعة والتاريخ
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    HomeHeaderDivider()
                    CastivioClock(
                        fsTime = frame.fsTitle,
                        fsDate = frame.fsChip,
                    )
                }
            }

            // الخط السفلي المتدرج باستخدام لون الهوية في الثيم
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.5.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                colors.primary,
                                colors.primary.copy(alpha = 0.35f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )
        }
    }
}

@Composable
private fun HomeHeaderAction(
    icon: ImageVector,
    label: String,
    frame: CastivioMetrics,
    onClick: () -> Unit,
) {
    val colors = CastivioTheme.colors

    InteractiveGlassCard(
        onClick = onClick,
        modifier = Modifier
            .widthIn(min = frame.touchTarget, max = 108.dp)
            .height(frame.touchTarget),
        shape = RoundedCornerShape(12.dp),
        fill = Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent)),
        restBorder = Color.Transparent,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = colors.onBackgroundVariant,
                modifier = Modifier.size(21.dp),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = castivioChipStyle(frame.fsChip),
                color = colors.onBackgroundMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HomeHeaderDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(34.dp)
            .background(CastivioTheme.colors.onBackground.copy(alpha = 0.18f)),
    )
}
