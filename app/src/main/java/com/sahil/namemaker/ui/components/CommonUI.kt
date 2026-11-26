package com.sahil.namemaker.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sahil.namemaker.ui.theme.*

@Composable
fun AppHeader(title: String, subtitle: String = "Made with ❤️ by Sahil") {
    Surface(
        color = DarkBg,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = title,
                            color = TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = subtitle,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 30.dp, top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SpinLoading(text: String = "Loading data...") {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = ElectricCyan,
                strokeWidth = 3.dp,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = text,
                color = TextSecondary,
                fontSize = 13.sp
            )
        }
    }
}

fun copyToClipboard(context: Context, text: String, label: String = "Copied to clipboard!") {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("NameMaker", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "✨ $label", Toast.LENGTH_SHORT).show()
}

@Composable
fun CopyButton(textToCopy: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    FilledTonalIconButton(
        onClick = { copyToClipboard(context, textToCopy) },
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = SurfaceElevated,
            contentColor = ElectricCyan
        ),
        modifier = modifier.size(36.dp)
    ) {
        Icon(
            imageVector = Icons.Default.ContentCopy,
            contentDescription = "Copy",
            modifier = Modifier.size(18.dp)
        )
    }
}
