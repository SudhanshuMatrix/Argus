package com.ajayinnovations.argus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ajayinnovations.argus.ui.theme.Edge
import com.ajayinnovations.argus.ui.theme.Ink
import com.ajayinnovations.argus.ui.theme.Line
import com.ajayinnovations.argus.ui.theme.Paper
import com.ajayinnovations.argus.ui.theme.Pine
import com.ajayinnovations.argus.ui.theme.PineTint

/** A 4dp coloured bar down the left: the app's way of saying "this one matters", instead of cards. */
@Composable
fun SideBar(color: Color, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Row(modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Box(Modifier.width(4.dp).fillMaxHeight().background(color))
        Column(
            modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

/** A thin rule between rows; no cards, no shadows. */
@Composable
fun Divider() = Box(Modifier.fillMaxWidth().height(1.dp).background(Line))

/**
 * One choice in a list, the whole row tappable (64dp). Picked: a Pine edge, a pale Pine fill
 * and a filled dot; TalkBack reads it as a selected radio button.
 */
@Composable
fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val edge = if (selected) Pine else Edge
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(if (selected) PineTint else Paper)
            .border(if (selected) 3.dp else 2.dp, edge, MaterialTheme.shapes.medium)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = 16.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.width(24.dp).height(24.dp).clip(CircleShape).border(2.dp, edge, CircleShape),
        ) {
            if (selected) Box(Modifier.width(12.dp).height(12.dp).clip(CircleShape).background(Pine))
        }
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = Ink,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

/** White panel for form fields and lists. */
@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(Paper).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}
