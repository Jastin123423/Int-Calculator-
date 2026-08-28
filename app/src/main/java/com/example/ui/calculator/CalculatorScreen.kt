package com.example.ui.calculator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CalcButton
import com.example.ui.components.CalcDisplay
import com.example.ui.components.QuickToolsBottomSheet
import com.example.ui.theme.ClearKeyRed
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EqualKeyDark
import com.example.ui.theme.OperatorKeyDark
import com.example.viewmodel.CalculatorViewModel

@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel,
    onNavigateToTool: (String) -> Unit,
    onNavigateToVault: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val hapticEnabled by viewModel.hapticsEnabled.collectAsState()

    var isQuickToolsOpen by remember { mutableStateOf(false) }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Calculator Display Area
            CalcDisplay(
                expression = uiState.expression,
                result = uiState.result,
                hasMemory = uiState.hasMemory,
                angleUnit = uiState.angleUnit.name,
                onAngleUnitToggle = { viewModel.toggleAngleUnit() },
                onQuickToolsClick = { isQuickToolsOpen = true },
                onShowStepsClick = { viewModel.openShowSteps() },
                onBackspaceClick = { viewModel.onBackspace() },
                onPasteText = { text -> viewModel.pasteNumber(text) },
                onVaultClick = onNavigateToVault,
                modifier = Modifier.weight(1f)
            )

            // Memory Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("MC", "MR", "M+", "M-", "MS").forEach { memBtn ->
                    TextButton(
                        onClick = {
                            when (memBtn) {
                                "MC" -> viewModel.memoryClear()
                                "MR" -> viewModel.memoryRecall()
                                "M+" -> viewModel.memoryAdd()
                                "M-" -> viewModel.memorySubtract()
                                "MS" -> viewModel.memoryStore()
                            }
                        }
                    ) {
                        Text(
                            text = memBtn,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (uiState.hasMemory && (memBtn == "MR" || memBtn == "M+"))
                                MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Scientific Expand Toggle Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (uiState.isScientificExpanded) "Scientific Functions" else "Standard Mode",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )

                IconButton(onClick = { viewModel.toggleScientificPanel() }) {
                    Icon(
                        imageVector = if (uiState.isScientificExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle Scientific Mode",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Expandable Scientific Panel
            AnimatedVisibility(
                visible = uiState.isScientificExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    val sciRows = listOf(
                        listOf("sin", "cos", "tan", "asin", "acos"),
                        listOf("sinh", "cosh", "tanh", "log", "ln"),
                        listOf("√", "∛", "x²", "x³", "xʸ"),
                        listOf("10ˣ", "eˣ", "1/x", "!", "abs"),
                        listOf("π", "e", "(", ")", "mod")
                    )

                    sciRows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            row.forEach { func ->
                                CalcButton(
                                    text = func,
                                    onClick = {
                                        when (func) {
                                            "sin", "cos", "tan", "asin", "acos", "sinh", "cosh", "tanh", "log", "ln", "abs" -> viewModel.onInput("$func(")
                                            "√" -> viewModel.onInput("√(")
                                            "∛" -> viewModel.onInput("∛(")
                                            "x²" -> viewModel.onInput("^2")
                                            "x³" -> viewModel.onInput("^3")
                                            "xʸ" -> viewModel.onInput("^")
                                            "10ˣ" -> viewModel.onInput("10^")
                                            "eˣ" -> viewModel.onInput("e^")
                                            "1/x" -> viewModel.onInput("1/")
                                            "!" -> viewModel.onInput("!")
                                            "π" -> viewModel.onInput("π")
                                            "e" -> viewModel.onInput("e")
                                            "mod" -> viewModel.onInput(" mod ")
                                            else -> viewModel.onInput(func)
                                        }
                                    },
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.secondary,
                                    hapticsEnabled = hapticEnabled,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Standard Calculator Keyboard
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                val padRows = listOf(
                    listOf("AC", "( )", "%", "÷"),
                    listOf("7", "8", "9", "×"),
                    listOf("4", "5", "6", "−"),
                    listOf("1", "2", "3", "+"),
                    listOf("0", ".", "⌫", "=")
                )

                padRows.forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { key ->
                            val isOp = key == "÷" || key == "×" || key == "−" || key == "+" || key == "( )" || key == "%"
                            val isEq = key == "="
                            val isAc = key == "AC"

                            val containerColor = when {
                                isEq -> CyanAccent
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }

                            val contentColor = when {
                                isEq -> Color(0xFF0A0C10)
                                isAc -> com.example.ui.theme.ClearKeyRed
                                isOp -> CyanAccent
                                else -> MaterialTheme.colorScheme.onSurface
                            }

                            CalcButton(
                                text = key,
                                onClick = {
                                    when (key) {
                                        "AC" -> viewModel.onClear()
                                        "=" -> viewModel.onEqual()
                                        "−" -> viewModel.onInput(" - ")
                                        "+" -> viewModel.onInput(" + ")
                                        "×" -> viewModel.onInput(" × ")
                                        "÷" -> viewModel.onInput(" ÷ ")
                                        "%" -> viewModel.onInput("%")
                                        "( )" -> {
                                            val expr = viewModel.uiState.value.expression
                                            val openCount = expr.count { it == '(' }
                                            val closeCount = expr.count { it == ')' }
                                            if (openCount > closeCount && expr.lastOrNull()?.isDigit() == true) {
                                                viewModel.onInput(")")
                                            } else {
                                                viewModel.onInput("(")
                                            }
                                        }
                                        "⌫" -> viewModel.onBackspace()
                                        else -> viewModel.onInput(key)
                                    }
                                },
                                containerColor = containerColor,
                                contentColor = contentColor,
                                isAccent = isEq,
                                hapticsEnabled = hapticEnabled,
                                tag = "btn_$key",
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Quick Tools Bottom Sheet
    if (isQuickToolsOpen) {
        QuickToolsBottomSheet(
            onDismissRequest = { isQuickToolsOpen = false },
            onToolSelect = { route -> onNavigateToTool(route) }
        )
    }

    // Show Steps Modal Dialog
    if (uiState.isShowStepsOpen) {
        AlertDialog(
            onDismissRequest = { viewModel.closeShowSteps() },
            title = {
                Text(
                    text = "Calculation Steps",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "Expression: ${uiState.expression}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(modifier = Modifier.height(200.dp)) {
                        items(uiState.steps) { step ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Step ${step.stepNumber}: ${step.description}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "= ${step.intermediateResult}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.closeShowSteps() }) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
