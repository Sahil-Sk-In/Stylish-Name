package com.sahil.namemaker.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sahil.namemaker.data.model.Decoration
import com.sahil.namemaker.data.model.KeyboardFont
import com.sahil.namemaker.data.model.SymbolCategory
import com.sahil.namemaker.data.repository.AppRepository
import com.sahil.namemaker.engine.FontTransformer
import com.sahil.namemaker.ui.components.CopyButton
import com.sahil.namemaker.ui.components.SpinLoading
import com.sahil.namemaker.ui.components.copyToClipboard
import com.sahil.namemaker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen(repository: AppRepository) {
    val context = LocalContext.current

    // State
    var isLoading by remember { mutableStateOf(true) }
    var inputText by remember { mutableStateOf("Sahil") }
    var selectedIntro by remember { mutableStateOf("") }
    var selectedOutro by remember { mutableStateOf("") }

    var decorations by remember { mutableStateOf<List<Decoration>>(emptyList()) }
    var keyboards by remember { mutableStateOf<List<KeyboardFont>>(emptyList()) }
    var customCategories by remember { mutableStateOf<List<SymbolCategory>>(emptyList()) }

    var showDecorDrawer by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Load initial crunch data & custom categories with 0ms in-memory cache
    LaunchedEffect(Unit) {
        val (decors, fonts) = repository.getCrunchData()
        decorations = decors
        keyboards = fonts
        customCategories = repository.getCustomCategories()
        isLoading = false
    }

    if (isLoading) {
        SpinLoading(text = "Loading font engine & decorations...")
        return
    }

    // Generate styled variations in real-time
    val styledVariations = remember(inputText, keyboards, selectedIntro, selectedOutro) {
        FontTransformer.generateAllStyles(
            text = inputText,
            fonts = keyboards,
            intro = selectedIntro,
            outro = selectedOutro
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // Top Section: Real-time Text Input & Action Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Surface(
                color = CardBg,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = "Enter text to stylize...",
                                color = TextMuted,
                                fontSize = 15.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (inputText.isNotEmpty()) {
                                IconButton(onClick = { inputText = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = ElectricCyan
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { showDecorDrawer = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SurfaceElevated,
                                contentColor = ElectricCyan
                            ),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, CardBorder),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Decorations & Symbols",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (selectedIntro.isNotEmpty() || selectedOutro.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    selectedIntro = ""
                                    selectedOutro = ""
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text(
                                    text = "Reset Frame",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Font Cards List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(styledVariations) { (fontName, transformedText) ->
                Surface(
                    color = CardBg,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = fontName,
                                color = NeonPurple,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = transformedText,
                                color = TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Normal,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        CopyButton(textToCopy = transformedText)
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet: Symbols & Decoration Categories
    if (showDecorDrawer) {
        ModalBottomSheet(
            onDismissRequest = { showDecorDrawer = false },
            sheetState = sheetState,
            containerColor = CardBg,
            scrimColor = DarkBg.copy(alpha = 0.7f)
        ) {
            DecorationBottomSheetContent(
                decorations = decorations,
                customCategories = customCategories,
                selectedIntro = selectedIntro,
                selectedOutro = selectedOutro,
                onSelectIntro = { selectedIntro = it },
                onSelectOutro = { selectedOutro = it },
                onInsertSymbol = { symbol ->
                    inputText += symbol
                    Toast.makeText(context, "Added $symbol", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
fun DecorationBottomSheetContent(
    decorations: List<Decoration>,
    customCategories: List<SymbolCategory>,
    selectedIntro: String,
    selectedOutro: String,
    onSelectIntro: (String) -> Unit,
    onSelectOutro: (String) -> Unit,
    onInsertSymbol: (String) -> Unit
) {
    val intros = remember(decorations) { decorations.filter { it.type == "intro" } }
    val outros = remember(decorations) { decorations.filter { it.type == "outro" } }

    val tabs = remember(customCategories) {
        listOf("Intros", "Outros") + customCategories.map { it.name }
    }
    var selectedTab by remember { mutableStateOf("Intros") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.75f)
            .padding(16.dp)
    ) {
        Text(
            text = "Select Frame / Symbol",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Horizontal Category Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabs.forEach { tabName ->
                val isSelected = selectedTab == tabName
                Surface(
                    onClick = { selectedTab = tabName },
                    color = if (isSelected) ElectricCyan else SurfaceElevated,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, if (isSelected) ElectricCyan else CardBorder)
                ) {
                    Text(
                        text = tabName,
                        color = if (isSelected) DarkBg else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTab) {
            "Intros" -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(intros) { item ->
                        val isSelected = selectedIntro == item.text
                        Surface(
                            onClick = { onSelectIntro(item.text) },
                            color = if (isSelected) SurfaceElevated else CardBg,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (isSelected) ElectricCyan else CardBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.text,
                                    color = if (isSelected) ElectricCyan else TextPrimary,
                                    fontSize = 15.sp
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = ElectricCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            "Outros" -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(outros) { item ->
                        val isSelected = selectedOutro == item.text
                        Surface(
                            onClick = { onSelectOutro(item.text) },
                            color = if (isSelected) SurfaceElevated else CardBg,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (isSelected) NeonPurple else CardBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.text,
                                    color = if (isSelected) NeonPurple else TextPrimary,
                                    fontSize = 15.sp
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = NeonPurple,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                val category = customCategories.find { it.name == selectedTab }
                val symbols = category?.items ?: emptyList()
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 54.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(symbols) { symbol ->
                        Surface(
                            onClick = { onInsertSymbol(symbol) },
                            color = SurfaceElevated,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, CardBorder)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = symbol,
                                    color = TextPrimary,
                                    fontSize = 20.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
