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
                val base = when {
                    justEvaluated && isOperator ->
                        if (current.result.isNotEmpty() && current.result != "Error") current.result
                        else current.expression
                    justEvaluated -> ""
                    else -> current.expression
                }
                justEvaluated = false
                val newExpression = appendInput(base, label)
                val liveResult = evaluatePreview(newExpression)
                _uiState.value = current.copy(expression = newExpression, result = liveResult)
            }
        }
    }
    private val binaryOps = setOf("+", "-", "×", "÷", "^")

    private fun appendInput(expr: String, label: String): String {
        val last = expr.lastOrNull()?.toString()
        // After % or ) a new number means multiply: 5%2 → 5%×2, (2+3)4 → (2+3)×4
        val afterClosed = last == "%" || last == ")"
        return when {
            label == "()" -> {
                // One smart key: closes an open bracket after a number, otherwise opens one.
                // A bracket right after a number multiplies: 2( → 2×(
                val stillOpen = expr.count { it == '(' } - expr.count { it == ')' }
                val c = expr.lastOrNull()
                val afterValue = c != null && (c.isDigit() || c == ')' || c == '%' || c == '.')
                when {
                    stillOpen > 0 && afterValue -> expr.trimEnd('.') + ")"
                    afterValue -> expr.trimEnd('.') + "×("
                    else -> "$expr("
                }
            }
            label in binaryOps -> {
                if (label == "-" && (last == null || last in setOf("×", "÷", "^", "("))) {
                    return expr + "-"
                }
                val trimmed = expr.trimEnd { it.toString() in binaryOps || it == '.' }
                // Nothing to work on yet: right at the start, or right after "("
                if (trimmed.isEmpty() || trimmed.last() == '(') expr else trimmed + label
            }
            label == "%" -> {
                val trimmed = expr.trimEnd { it.toString() in binaryOps || it == '.' }
                if (trimmed.isEmpty() || trimmed.last() == '%' || trimmed.last() == '(') expr
                else "$trimmed%"
            }
            label == "." -> {
                val currentNumber = expr.takeLastWhile { it.isDigit() || it == '.' }
                when {
                    '.' in currentNumber -> expr
                    currentNumber.isEmpty() -> expr + (if (afterClosed) "×0." else "0.")
                    else -> "$expr."
                }
            }
            else -> if (afterClosed) "$expr×$label" else expr + label
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
        val trailingOperator = Regex("[+\\-×÷^(]$")
        return !trailingOperator.containsMatchIn(expr)
    }

    /** Adds the ")" still missing at the end, so (2+3 can already show 5. */
    private fun closeBrackets(expr: String): String {
        val stillOpen = expr.count { it == '(' } - expr.count { it == ')' }
        return if (stillOpen > 0) expr + ")".repeat(stillOpen) else expr
    }

    private fun evaluatePreview(expression: String): String {
        if (!isCompleteExpression(expression)) return ""
        return calculate(closeBrackets(expression)) ?: ""
    }

    private fun evaluate() {
        val typed = _uiState.value.expression
        if (!isCompleteExpression(typed)) {
            _uiState.value = _uiState.value.copy(result = "Error")
            return
        }
        val expression = closeBrackets(typed)
        val resStr = calculate(expression)
        if (resStr == null) {
            _uiState.value = _uiState.value.copy(result = "Error")
            return
        }
        justEvaluated = true
        _uiState.value = _uiState.value.copy(expression = expression, result = resStr)

        viewModelScope.launch {
            repository.save(expression = expression, result = resStr)
        }
    }

    /**
     * The answer as display text, or null when it can't be worked out.
     * Keypad maths is done exactly (see [ExactMath]); exp4j is only the fallback,
     * e.g. for a result like 1.5E20 reused from history.
     */
    private fun calculate(expression: String): String? {
        ExactMath.evaluate(expression)?.let { return formatExact(it) }
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

            if (res.isNaN() || res.isInfinite()) null else formatNumber(res)
        } catch (e: Exception) {
            null
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