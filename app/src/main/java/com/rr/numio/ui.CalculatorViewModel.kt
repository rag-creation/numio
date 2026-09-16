package com.rr.numio.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rr.numio.data.AppDatabase
import com.rr.numio.data.HistoryEntity
import com.rr.numio.data.HistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.objecthunter.exp4j.ExpressionBuilder
import net.objecthunter.exp4j.function.Function

private val sqrtFunction = object : Function("sqrt", 1) {
    override fun apply(vararg args: Double): Double = kotlin.math.sqrt(args[0])
}

data class CalculatorUiState(
    val expression: String = "",
    val result: String = "",
    val isHistoryVisible: Boolean = false,
    val isEasterEggVisible: Boolean = false
)

class CalculatorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = HistoryRepository(
        AppDatabase.getInstance(application).historyDao()
    )

    private val _uiState = MutableStateFlow(CalculatorUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState

    private var justEvaluated = false

    val history: StateFlow<List<HistoryEntity>> = repository.history.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val operators = setOf("+", "-", "×", "÷", "^", "%")

    fun onKeyPress(label: String) {
        val current = _uiState.value
        when (label) {
            "AC" -> {
                justEvaluated = false
                _uiState.value = current.copy(expression = "", result = "")
            }
            "C" -> {
                justEvaluated = false
                val newExpr = current.expression.dropLast(1)
                val liveResult = evaluatePreview(newExpr)
                _uiState.value = current.copy(expression = newExpr, result = liveResult)
            }
            "=" -> evaluate()
            else -> {
                val isOperator = label in operators
                val newExpression = when {
                    justEvaluated && isOperator -> current.result + label
                    justEvaluated -> label
                    else -> current.expression + label
                }
                justEvaluated = false
                val liveResult = evaluatePreview(newExpression)
                _uiState.value = current.copy(expression = newExpression, result = liveResult)
            }
        }
    }

    fun toggleHistory() {
        _uiState.value = _uiState.value.copy(isHistoryVisible = !_uiState.value.isHistoryVisible)
    }

    fun clearHistory() {
        viewModelScope.launch { repository.clear() }
    }

    fun reuseHistoryEntry(entry: HistoryEntity) {
        justEvaluated = true
        _uiState.value = _uiState.value.copy(
            expression = entry.result,
            result = "",
            isHistoryVisible = false
        )
    }

    fun triggerEasterEgg() {
        _uiState.value = _uiState.value.copy(isEasterEggVisible = true)
    }

    fun dismissEasterEgg() {
        _uiState.value = _uiState.value.copy(isEasterEggVisible = false)
    }

    private fun isCompleteExpression(expr: String): Boolean {
        if (expr.isBlank()) return false
        val trailingOperator = Regex("[+\\-×÷^]$")
        return !trailingOperator.containsMatchIn(expr)
    }

    private fun evaluatePreview(expression: String): String {
        if (!isCompleteExpression(expression)) return ""
        return try {
            val sanitized = expression
                .replace("×", "*")
                .replace("÷", "/")
                .replace("%", "/100")
                .replace(Regex("√(\\d+(\\.\\d+)?)"), "sqrt($1)")

            val res = ExpressionBuilder(sanitized)
                .function(sqrtFunction)
                .build()
                .evaluate()

            if (res.isNaN() || res.isInfinite()) ""
            else if (res % 1 == 0.0) res.toLong().toString()
            else res.toString()
        } catch (e: Exception) {
            ""
        }
    }

    private fun evaluate() {
        val expression = _uiState.value.expression
        if (!isCompleteExpression(expression)) {
            _uiState.value = _uiState.value.copy(result = "Error")
            return
        }
        try {
            val sanitized = expression
                .replace("×", "*")
                .replace("÷", "/")
                .replace("%", "/100")
                .replace(Regex("√(\\d+(\\.\\d+)?)"), "sqrt($1)")

            val res = ExpressionBuilder(sanitized)
                .function(sqrtFunction)
                .build()
                .evaluate()

            if (res.isNaN() || res.isInfinite()) {
                _uiState.value = _uiState.value.copy(result = "Error")
                return
            }

            val resStr = if (res % 1 == 0.0) res.toLong().toString() else res.toString()
            justEvaluated = true
            _uiState.value = _uiState.value.copy(result = resStr)

            viewModelScope.launch {
                repository.save(expression = expression, result = resStr)
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(result = "Error")
        }
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CalculatorViewModel(application) as T
                }
            }
    }
}