package com.example.ui.tools.unitconverter

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.converter.UnitCategory
import com.example.domain.converter.UnitConverter
import com.example.domain.converter.UnitItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitConverterScreen(
    onNavigateBack: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf(UnitCategory.LENGTH) }
    var availableUnits by remember(selectedCategory) {
        mutableStateOf(UnitConverter.getUnitsForCategory(selectedCategory))
    }

    var fromUnit by remember(availableUnits) { mutableStateOf(availableUnits[0]) }
    var toUnit by remember(availableUnits) { mutableStateOf(availableUnits[1]) }

    var inputValue by remember { mutableStateOf("1") }
    var rotationAngle by remember { mutableStateOf(0f) }
    val animatedRotation by animateFloatAsState(targetValue = rotationAngle, label = "swap_rotation")

    val numValue = inputValue.toDoubleOrNull() ?: 0.0
    val convertedResult = UnitConverter.convertFormatted(numValue, fromUnit, toUnit, selectedCategory)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Unit Converter", fontWeight = FontWeight.Bold) },
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
            // Category Selector Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                UnitCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = cat == selectedCategory,
                        onClick = {
                            selectedCategory = cat
                            availableUnits = UnitConverter.getUnitsForCategory(cat)
                            fromUnit = availableUnits[0]
                            toUnit = availableUnits[if (availableUnits.size > 1) 1 else 0]
                        },
                        label = { Text(cat.displayName) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Input Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Value", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = inputValue,
                        onValueChange = { inputValue = it },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("unit_input_field")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // From Unit Selector
                    UnitDropdownSelector(
                        label = "From Unit",
                        selectedUnit = fromUnit,
                        unitList = availableUnits,
                        onUnitSelected = { fromUnit = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Animated Swap Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = {
                        rotationAngle += 180f
                        val temp = fromUnit
                        fromUnit = toUnit
                        toUnit = temp
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Swap Units",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .rotate(animatedRotation)
                            .padding(4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Output Result Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // To Unit Selector
                    UnitDropdownSelector(
                        label = "To Unit",
                        selectedUnit = toUnit,
                        unitList = availableUnits,
                        onUnitSelected = { toUnit = it }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Result", fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$convertedResult ${toUnit.symbol}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitDropdownSelector(
    label: String,
    selectedUnit: UnitItem,
    unitList: List<UnitItem>,
    onUnitSelected: (UnitItem) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = "${selectedUnit.name} (${selectedUnit.symbol})",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            unitList.forEach { unit ->
                DropdownMenuItem(
                    text = { Text("${unit.name} (${unit.symbol})") },
                    onClick = {
                        onUnitSelected(unit)
                        expanded = false
                    }
                )
            }
        }
    }
}
