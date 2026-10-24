package com.ajayinnovations.argus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.ajayinnovations.argus.ui.theme.Line
import com.ajayinnovations.argus.ui.theme.Pine

/** One segment per wizard step: done and current in Pine, the rest in Line. The "Step X of N" text says it for TalkBack. */
@Composable
fun SegmentedProgress(current: Int, total: Int, modifier: Modifier = Modifier) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = modifier.fillMaxWidth().clearAndSetSemantics { }) {
        repeat(total) { i ->
            Box(
                Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (i < current) Pine else Line),
            )
        }
    }
}
