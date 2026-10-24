package com.ajayinnovations.argus.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.ajayinnovations.argus.R
import com.ajayinnovations.argus.ui.theme.Line
import com.ajayinnovations.argus.ui.theme.Paper
import com.ajayinnovations.argus.ui.theme.Pine
import com.ajayinnovations.argus.ui.theme.PineTint
import com.ajayinnovations.argus.ui.theme.Slate

/** The app's four places. */
enum class Tab(@DrawableRes val icon: Int, @StringRes val label: Int) {
    HOME(R.drawable.ic_nav_home, R.string.tab_home),
    CHECK(R.drawable.ic_nav_check, R.string.tab_check),
    SETTINGS(R.drawable.ic_nav_settings, R.string.nav_settings),
    ABOUT(R.drawable.ic_nav_about, R.string.nav_about),
}

/**
 * Bottom navigation: icon over an 18sp label for every tab (never an icon alone). The picked
 * tab gets a Pine icon on a pale Pine pill and a bold label. Built by hand, not Material's
 * NavigationBar, whose fixed 80dp height clips 18sp labels at large font sizes.
 */
@Composable
fun BottomNav(selected: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    // Four labels share one row: they grow with the phone's text size up to 130% (about 23sp),
    // then stop, so "Settings" never gets cut. Everything else on screen scales fully.
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, minOf(density.fontScale, MAX_FONT_SCALE))) {
        Bar(selected, onSelect, modifier)
    }
}

private const val MAX_FONT_SCALE = 1.3f

@Composable
private fun Bar(selected: Tab, onSelect: (Tab) -> Unit, modifier: Modifier) {
    Column(modifier.fillMaxWidth().background(Paper)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Tab.entries.forEach { tab -> TabItem(tab, tab == selected) { onSelect(tab) } }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.TabItem(tab: Tab, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) Pine else Slate
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(12.dp))
            .selectable(selected = selected, onClick = onClick, role = Role.Tab)
            .padding(vertical = 4.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(64.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (selected) PineTint else Paper),
        ) {
            // The label below says it in words, so the icon itself is silent for TalkBack
            Icon(painterResource(tab.icon), contentDescription = null, tint = color, modifier = Modifier.size(26.dp))
        }
        Text(
            stringResource(tab.label),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            color = color,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}
