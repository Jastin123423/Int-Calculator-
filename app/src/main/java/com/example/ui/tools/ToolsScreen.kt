package com.example.ui.tools

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ToolHubItem(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val route: String
)

@Composable
fun ToolsScreen(
    onNavigateToTool: (String) -> Unit
) {
    val toolList = listOf(
        ToolHubItem(
            "unit",
            "Unit Converter",
            "Convert Length, Area, Volume, Weight, Temp, Speed, Data & 7 more",
            Icons.Default.SwapHoriz,
            "tool_unit_converter"
        ),
        ToolHubItem(
            "currency",
            "Currency Converter",
            "Real-time & offline cached rates for 15 major currencies",
            Icons.Default.CurrencyExchange,
            "tool_currency_converter"
        ),
        ToolHubItem(
            "percentage",
            "Percentage Calculator",
            "Calculate X% of Y, % difference, increase/decrease, % change",
            Icons.Default.Percent,
            "tool_percentage"
        ),
        ToolHubItem(
            "finance",
            "Finance & Loans",
            "Loan repayment, Mortgage, Simple & Compound Interest, Savings, Tip, Tax",
            Icons.Default.Calculate,
            "tool_finance"
        ),
        ToolHubItem(
            "datetime",
            "Date & Time Calculator",
            "Calculate date difference, age in years/days, add/subtract days",
            Icons.Default.CalendarToday,
            "tool_datetime"
        ),
        ToolHubItem(
            "equation",
            "Equation Solver",
            "Solve linear & quadratic equations with step-by-step breakdown",
            Icons.Default.Functions,
            "tool_equation_solver"
        ),
        ToolHubItem(
            "camera",
            "Camera Math Solver",
            "Scan or upload a photo of a math problem to extract & solve",
            Icons.Default.CameraAlt,
            "tool_math_solver"
        ),
        ToolHubItem(
            "ai",
            "AI Math Assistant",
            "Ask natural language math questions & receive step-by-step guidance",
            Icons.Default.AutoAwesome,
            "tool_ai_assistant"
        ),
        ToolHubItem(
            "vault",
            "Private Vault",
            "Encrypted local vault for private photos, videos, contacts & albums",
            Icons.Default.Security,
            "vault_auth"
        )
    )

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "Int Calculator Tools Suite",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Comprehensive utility calculators for finance, math & science",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(toolList) { tool ->
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToTool(tool.route) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = tool.icon,
                                contentDescription = tool.title,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Text(
                                    text = tool.title,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = tool.description,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
