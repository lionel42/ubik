package io.github.ubik.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import io.github.ubik.R

@Composable
fun UbikLogo(
    modifier: Modifier = Modifier.width(132.dp).height(40.dp),
    onClick: (() -> Unit)? = null,
    contentDescription: String = "Ubik"
) {
    val logoModifier = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }

    Image(
        painter = painterResource(id = R.drawable.app_menu_logo),
        contentDescription = contentDescription,
        modifier = logoModifier
    )
}