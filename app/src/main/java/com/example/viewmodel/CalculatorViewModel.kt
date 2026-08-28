package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.preferences.PreferencesManager
import com.example.data.repository.HistoryRepository
import com.example.domain.calculator.AngleUnit
import com.example.domain.calculator.CalculationResult
import com.example.domain.calculator.CalculationStep
import com.example.domain.calculator.ExpressionParser
import com.example.domain.calculator.StepByStepSolver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CalculatorUiState(
    val expression: String = "",
    val result: String = "0",
    val memoryValue: Double = 0.0,
    val hasMemory: Boolean = false,
    val angleUnit: AngleUnit = AngleUnit.DEG,
    val isScientificExpanded: Boolean = false,
    val steps: List<CalculationStep> = emptyList(),
    val isShowStepsOpen: Boolean = false,
    val errorMessage: String? = null
)

class CalculatorViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesManager = PreferencesManager(application)
    private val historyRepository = HistoryRepository(
        AppDatabase.getDatabase(application).historyDao()
    )

    private val _expression = MutableStateFlow("")
    private val _result = MutableStateFlow("0")
    private val _memoryValue = MutableStateFlow(0.0)
    private val _angleUnit = MutableStateFlow(AngleUnit.DEG)
    private val _isScientificExpanded = MutableStateFlow(false)
    private val _steps = MutableStateFlow<List<CalculationStep>>(emptyList())
    private val _isShowStepsOpen = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    private val _coreState = combine(_expression, _result, _memoryValue, _angleUnit) { expr, res, mem, unit ->
        QuadCore(expr, res, mem, unit)
    }

    private val _uiFlagsState = combine(_isScientificExpanded, _steps, _isShowStepsOpen, _errorMessage) { isSci, steps, isStepsOpen, err ->
        QuadFlags(isSci, steps, isStepsOpen, err)
    }

    val uiState: StateFlow<CalculatorUiState> = combine(_coreState, _uiFlagsState) { core, flags ->
        CalculatorUiState(
            expression = core.expression,
            result = core.result,
            memoryValue = core.memoryValue,
            hasMemory = core.memoryValue != 0.0,
            angleUnit = core.angleUnit,
            isScientificExpanded = flags.isScientificExpanded,
            steps = flags.steps,
            isShowStepsOpen = flags.isShowStepsOpen,
            errorMessage = flags.errorMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalculatorUiState()
    )

    private data class QuadCore(
        val expression: String,
        val result: String,
        val memoryValue: Double,
        val angleUnit: AngleUnit
    )

    private data class QuadFlags(
        val isScientificExpanded: Boolean,
        val steps: List<CalculationStep>,
        val isShowStepsOpen: Boolean,
        val errorMessage: String?
    )

    val hapticsEnabled = preferencesManager.hapticsFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
    )

    fun onInput(char: String) {
        val currentExpr = _expression.value
        _expression.value = currentExpr + char
        evaluateCurrent()
    }

    fun onClear() {
        _expression.value = ""
        _result.value = "0"
        _steps.value = emptyList()
        _errorMessage.value = null
    }

    fun onBackspace() {
        val current = _expression.value
        if (current.isNotEmpty()) {
            _expression.value = current.dropLast(1)
            evaluateCurrent()
        }
    }

    fun onTogglePlusMinus() {
        val current = _expression.value
        if (current.isBlank()) return
        if (current.startsWith("-")) {
            _expression.value = current.drop(1)
        } else {
            _expression.value = "-$current"
        }
        evaluateCurrent()
    }

    fun onEqual() {
        val expr = _expression.value
        if (expr.isBlank()) return

        val calcResult = ExpressionParser.evaluate(expr, _angleUnit.value)
        when (calcResult) {
            is CalculationResult.Success -> {
                val finalFormatted = calcResult.formattedValue
                _result.value = finalFormatted

                viewModelScope.launch {
                    historyRepository.insert(
                        expression = expr,
                        result = finalFormatted,
                        category = if (_isScientificExpanded.value) "Scientific" else "Standard"
                    )
                }

                _steps.value = StepByStepSolver.generateSteps(expr, _angleUnit.value)
                _expression.value = finalFormatted
            }
            is CalculationResult.Error -> {
                _result.value = calcResult.message
            }
        }
    }

    private fun evaluateCurrent() {
        val expr = _expression.value
        if (expr.isBlank()) {
            _result.value = "0"
            return
        }
        val calcResult = ExpressionParser.evaluate(expr, _angleUnit.value)
        when (calcResult) {
            is CalculationResult.Success -> _result.value = calcResult.formattedValue
            is CalculationResult.Error -> {}
        }
    }

    fun memoryClear() {
        _memoryValue.value = 0.0
    }

    fun memoryRecall() {
        val memStr = ExpressionParser.formatNumber(_memoryValue.value)
        _expression.value += memStr
        evaluateCurrent()
    }

    fun memoryAdd() {
        val currentRes = _result.value.replace(",", "").toDoubleOrNull() ?: 0.0
        _memoryValue.value += currentRes
    }

    fun memorySubtract() {
        val currentRes = _result.value.replace(",", "").toDoubleOrNull() ?: 0.0
        _memoryValue.value -= currentRes
    }

    fun memoryStore() {
        val currentRes = _result.value.replace(",", "").toDoubleOrNull() ?: 0.0
        _memoryValue.value = currentRes
    }

    fun toggleAngleUnit() {
        _angleUnit.value = when (_angleUnit.value) {
            AngleUnit.DEG -> AngleUnit.RAD
            AngleUnit.RAD -> AngleUnit.GRAD
            AngleUnit.GRAD -> AngleUnit.DEG
        }
        evaluateCurrent()
    }

    fun toggleScientificPanel() {
        _isScientificExpanded.value = !_isScientificExpanded.value
    }

    fun openShowSteps() {
        if (_steps.value.isEmpty() && _expression.value.isNotBlank()) {
            _steps.value = StepByStepSolver.generateSteps(_expression.value, _angleUnit.value)
        }
        _isShowStepsOpen.value = true
    }

    fun closeShowSteps() {
        _isShowStepsOpen.value = false
    }

    fun pasteNumber(text: String) {
        val clean = text.filter { it.isDigit() || it == '.' || it == '-' || it == '+' || it == '*' || it == '/' || it == '(' || it == ')' }
        _expression.value += clean
        evaluateCurrent()
    }

    fun loadExpression(expr: String, res: String) {
        _expression.value = expr
        _result.value = res
        evaluateCurrent()
    }
}
