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
    /** Where the next key goes. Tap the expression to move it; normally it's at the end. */
    val cursor: Int = 0,
    /** True after tapping the expression: the cursor stays visible (even at the end) until = or AC. */
    val showCursor: Boolean = false,
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
        val cursor = current.cursor.coerceIn(0, current.expression.length)
        // Keys work on the text left of the cursor; the text right of it stays as it is.
        val before = current.expression.substring(0, cursor)
        val after = current.expression.substring(cursor)
        when (label) {
            "AC" -> {
                justEvaluated = false
                _uiState.value = current.copy(expression = "", result = "", cursor = 0, showCursor = false)
            }
            "C" -> {
                justEvaluated = false
                val newBefore = before.dropLast(1)
                val newExpr = newBefore + after
                _uiState.value = current.copy(
                    expression = newExpr,
                    result = evaluatePreview(newExpr),
                    cursor = newBefore.length
                )
            }
            "=" -> evaluate()
            else -> {
                val isOperator = label in operators
                val (base, rest) = when {
                    justEvaluated && isOperator ->
                        (if (current.result.isNotEmpty() && current.result != "Error") current.result
                        else current.expression) to ""
                    justEvaluated -> "" to ""
                    else -> before to after
                }
                justEvaluated = false
                var newBefore = appendInput(base, label)
                var newAfter = rest
                if (label == "()" && newBefore == "$base)" && rest.startsWith(")")) {
                    // A ")" is already right after the cursor: step over it, don't add another.
                    newBefore = base + ")"
                    newAfter = rest.drop(1)
                } else if (label in binaryOps && newBefore != base &&
                    rest.firstOrNull()?.toString() in binaryOps
                ) {
                    // Cursor before an operator (11|+12) and a new operator typed: replace it.
                    newAfter = rest.drop(1)
                }
                val newExpression = newBefore + newAfter
                _uiState.value = current.copy(
                    expression = newExpression,
                    result = evaluatePreview(newExpression),
                    cursor = newBefore.length
                )
            }
        }
    }

    /** Tapping the expression puts the cursor there, to fix or add a number in the middle. */
    fun moveCursor(position: Int) {
        val current = _uiState.value
        val newCursor = position.coerceIn(0, current.expression.length)
        if (newCursor == current.cursor && current.showCursor && !justEvaluated) return
        justEvaluated = false
        _uiState.value = current.copy(
            cursor = newCursor,
            showCursor = true,
            result = evaluatePreview(current.expression)
        )
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
            cursor = entry.result.length,
            showCursor = false,
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
        _uiState.value = _uiState.value.copy(
            expression = expression,
            result = resStr,
            cursor = expression.length,
            showCursor = false
        )

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