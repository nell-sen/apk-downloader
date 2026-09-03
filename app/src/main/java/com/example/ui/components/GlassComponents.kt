package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.GlassTokens
import com.example.ui.theme.LocalGlassColors

@Composable
fun GlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val glassColors = LocalGlassColors.current
    Surface(
        modifier = modifier.fillMaxSize(),
        color = glassColors.background,
        content = {
            Box(modifier = Modifier.fillMaxSize(), content = content)
        }
    )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(GlassTokens.CornerRadiusMd),
    borderColor: Color = LocalGlassColors.current.glassCardBorder,
    backgroundColor: Color = LocalGlassColors.current.glassCard,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = shape,
        color = backgroundColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    testTag: String = "glass_button"
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .testTag(testTag)
            .heightIn(min = GlassTokens.MinTouchTarget),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(GlassTokens.CornerRadiusSm),
        colors = ButtonDefaults.buttonColors(
            containerColor = AccentBlue,
            contentColor = Color.White
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp
        )
    }
}

@Composable
fun GlassSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    testTag: String = "glass_sec_button"
) {
    val glassColors = LocalGlassColors.current
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .testTag(testTag)
            .heightIn(min = GlassTokens.MinTouchTarget),
        shape = RoundedCornerShape(GlassTokens.CornerRadiusSm),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = glassColors.textPrimary
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, glassColors.glassCardBorder)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp
        )
    }
}

@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    testTag: String = "glass_text_field"
) {
    val glassColors = LocalGlassColors.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .testTag(testTag)
            .fillMaxWidth()
            .heightIn(min = GlassTokens.MinTouchTarget),
        shape = RoundedCornerShape(GlassTokens.CornerRadiusSm),
        placeholder = {
            Text(
                text = placeholder,
                color = glassColors.textSecondary,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingIcon = leadingIcon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = glassColors.textSecondary
                )
            }
        },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(
                    onClick = { onValueChange("") },
                    modifier = Modifier.size(GlassTokens.MinTouchTarget)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear text",
                        tint = glassColors.textSecondary
                    )
                }
            } else if (trailingIcon != null && onTrailingIconClick != null) {
                IconButton(
                    onClick = onTrailingIconClick,
                    modifier = Modifier.size(GlassTokens.MinTouchTarget)
                ) {
                    Icon(
                        imageVector = trailingIcon,
                        contentDescription = "Action",
                        tint = glassColors.textSecondary
                    )
                }
            }
        },
        textStyle = TextStyle(
            color = glassColors.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AccentBlue,
            unfocusedBorderColor = glassColors.glassCardBorder,
            focusedContainerColor = glassColors.surface,
            unfocusedContainerColor = glassColors.surface,
            cursorColor = AccentBlue
        ),
        singleLine = true,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions
    )
}

@Composable
fun GlassChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    testTag: String = "glass_chip"
) {
    val glassColors = LocalGlassColors.current
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(text) },
        leadingIcon = if (icon != null) {
            { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) }
        } else null,
        modifier = modifier.testTag(testTag),
        shape = RoundedCornerShape(GlassTokens.CornerRadiusSm),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = AccentBlue.copy(alpha = 0.1f),
            selectedLabelColor = AccentBlue,
            selectedLeadingIconColor = AccentBlue,
            containerColor = glassColors.glassCard,
            labelColor = glassColors.textSecondary,
            iconColor = glassColors.textSecondary
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = isSelected,
            borderColor = glassColors.glassCardBorder,
            selectedBorderColor = AccentBlue,
            borderWidth = 1.dp,
            selectedBorderWidth = 1.dp
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassTopBar(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val glassColors = LocalGlassColors.current
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = glassColors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = glassColors.textSecondary
                    )
                }
            }
        },
        modifier = modifier,
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = glassColors.surface,
            titleContentColor = glassColors.textPrimary,
            actionIconContentColor = glassColors.textPrimary
        )
    )
}
