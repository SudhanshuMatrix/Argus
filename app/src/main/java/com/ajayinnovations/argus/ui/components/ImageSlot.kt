package com.ajayinnovations.argus.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ajayinnovations.argus.BuildConfig
import com.ajayinnovations.argus.R

/**
 * A screenshot of an Android settings screen, dropped in later as
 * app/src/main/res/drawable-nodpi/[name].png. Until then: nothing in release builds, and a
 * labelled frame in dev builds so the slot is easy to find. The screen reads fine either way.
 */
@SuppressLint("DiscouragedApi") // looked up by name so a missing picture is simply skipped
@Composable
fun ImageSlot(name: String, description: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val id = remember(name) { context.resources.getIdentifier(name, "drawable", context.packageName) }
    when {
        id != 0 -> Image(
            painter = painterResource(id),
            contentDescription = description,
            contentScale = ContentScale.FillWidth,
            modifier = modifier.fillMaxWidth(),
        )
        BuildConfig.SHOW_IMAGE_SLOTS -> Text(
            text = stringResource(R.string.image_slot, name),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
                .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                .padding(16.dp),
        )
    }
}

/** Every image slot, so docs and code agree on the file names. */
object ImageSlots {
    const val RESTRICTED_SETTINGS = "setup_restricted_settings"
    const val TRANSSION_AUTOSTART = "setup_transsion_autostart"
    const val XIAOMI_AUTOSTART = "setup_xiaomi_autostart"
    const val XIAOMI_OTHER_PERMISSIONS = "setup_xiaomi_other_permissions"
}
