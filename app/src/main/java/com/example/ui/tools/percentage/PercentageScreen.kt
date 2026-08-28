package com.example.ui.tools.percentage

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.percentage.PercentageCalculator

enum class PercentageMode(val title: String) {
    PERCENT_OF("X% of Y"),
    IS_WHAT_PERCENT("X is what % of Y"),
    INCREASE("Increase by %"),
    DECREASE("Decrease by %"),
    DIFFERENCE("% Difference"),
    CHANGE("% Change")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PercentageScreen(
    onNavigateBack: () -> Unit
) {
    var mode by remember { mutableStateOf(PercentageMode.PERCENT_OF) }
    var val1 by remember { mutableStateOf("20") }
    var val2 by remember { mutableStateOf("500") }

    val num1 = val1.toDoubleOrNull() ?: 0.0
    val num2 = val2.toDoubleOrNull() ?: 0.0

    val (resultText, resultLabel) = when (mode) {
        PercentageMode.PERCENT_OF -> {
            val res = PercentageCalculator.percentageOf(num1, num2)
            Pair(PercentageCalculator.format(res), "$num1% of $num2")
        }
        PercentageMode.IS_WHAT_PERCENT -> {
            val res = PercentageCalculator.whatPercentage(num1, num2)
            Pair("${PercentageCalculator.format(res)}%", "$num1 of $num2")
        }
        PercentageMode.INCREASE -> {
            val res = PercentageCalculator.increaseByPercent(num1, num2)
            Pair(PercentageCalculator.format(res), "$num1 increased by $num2%")
        }
        PercentageMode.DECREASE -> {
            val res = PercentageCalculator.decreaseByPercent(num1, num2)
            Pair(PercentageCalculator.format(res), "$num1 decreased by $num2%")
        }
        PercentageMode.DIFFERENCE -> {
            val res = PercentageCalculator.percentageDifference(num1, num2)
            Pair("${PercentageCalculator.format(res)}%", "Difference between $num1 & $num2")
        }
        PercentageMode.CHANGE -> {
            val res = PercentageCalculator.percentageChange(num1, num2)
            Pair("${PercentageCalculator.format(res)}%", "Change from $num1 to $num2")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Percentage Calculator", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Mode selector chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PercentageMode.entries.forEach { m ->
                    FilterChip(
                        selected = m == mode,
                        onClick = { mode = m },
                        label = { Text(m.title) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    val label1 = when (mode) {
                        PercentageMode.PERCENT_OF -> "Percentage (%)"
                        PercentageMode.IS_WHAT_PERCENT -> "Part Value (X)"
                        PercentageMode.INCREASE, PercentageMode.DECREASE -> "Base Value"
                        PercentageMode.DIFFERENCE -> "First Value"
                        PercentageMode.CHANGE -> "Original Value (Old)"
                    }

                    val label2 = when (mode) {
                        PercentageMode.PERCENT_OF -> "Total Value (Y)"
                        PercentageMode.IS_WHAT_PERCENT -> "Total Value (Y)"
                        PercentageMode.INCREASE, PercentageMode.DECREASE -> "Percentage (%)"
                        PercentageMode.DIFFERENCE -> "Second Value"
                        PercentageMode.CHANGE -> "New Value"
                    }

                    OutlinedTextField(
                        value = val1,
                        onValueChange = { val1 = it },
                        label = { Text(label1) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = val2,
                        onValueChange = { val2 = it },
                        label = { Text(label2) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = resultLabel,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = resultText,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}
