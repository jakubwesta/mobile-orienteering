package com.mobileorienteering.ui.screens.auth.components

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.annotation.DrawableRes
import androidx.compose.ui.res.painterResource

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    @DrawableRes leadingIconRes: Int,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: (() -> Unit)? = null,
    enabled: Boolean = true
) {
    val focusManager = LocalFocusManager.current

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(painter = painterResource(id = leadingIconRes), contentDescription = label) },
        modifier = modifier,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(
            onNext = { onImeAction?.invoke() ?: focusManager.moveFocus(FocusDirection.Down) },
            onDone = { onImeAction?.invoke() }
        ),
        enabled = enabled
    )
}
