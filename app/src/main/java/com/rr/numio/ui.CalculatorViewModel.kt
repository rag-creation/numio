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

/** What each scientific key types. A function key types its name and an opening bracket. */
private val FUNCTION_KEYS = mapOf(
    "sin" to "sin(", "cos" to "cos(", "tan" to "tan(",
    "sin⁻¹" to "sin⁻¹(", "cos⁻¹" to "cos⁻¹(", "tan⁻¹" to "tan⁻¹(",
    "ln" to "ln(", "log" to "log(", "√" to "√(", "|x|" to "abs(",
    "eˣ" to "e^(", "10ˣ" to "10^(", "1/x" to "1÷("
)

/** Function names the cursor treats as one piece: ⌫ deletes all of "sin(", taps can't land inside it. */
private val FUNCTION_NAMES = listOf("sin⁻¹(", "cos⁻¹(", "tan⁻¹(", "sin(", "cos(", "tan(", "ln(", "log(", "abs(", "√(")

/** Characters that mean the old exp4j fallback can't be trusted (it would work in radians). */
private fun hasScientificSymbols(expression: String) =
    expression.any { it in 'a'..'z' || it == 'π' || it == '√' || it == '²' || it == '!' }

data class CalculatorUiState(
    val expression: String = "",
    val result: String = "",
    /** Where the next key goes. Tap the expression to move it; normally it's at the end. */
    val cursor: Int = 0,
    /** True after tapping the expression: the cursor stays visible (even at the end) until = or AC. */
    val showCursor: Boolean = false,
    /** True: sin, cos and tan work in degrees; false: in radians. */
    val degrees: Boolean = true,
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

    fun onKeyPress(key: String) {
        val label = if (key == "xʸ") "^" else key
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
                // A function like "sin(" goes in one tap
                val cut = FUNCTION_NAMES.firstOrNull { before.endsWith(it) }?.length ?: 1
                val newBefore = before.dropLast(cut)
                val newExpr = newBefore + after
                _uiState.value = current.copy(
                    expression = newExpr,
                    result = evaluatePreview(newExpr),
                    cursor = newBefore.length
                )
            }
            "=" -> evaluate()
            else -> {
                // x² and ! carry on from the last answer, like an operator
                val isOperator = label in operators || label == "x²" || label == "!"
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
        val newCursor = snapOutOfFunction(current.expression, position.coerceIn(0, current.expression.length))
        if (newCursor == current.cursor && current.showCursor && !justEvaluated) return
        justEvaluated = false
        _uiState.value = current.copy(
            cursor = newCursor,
            showCursor = true,
            result = evaluatePreview(current.expression)
        )
    }

    /** A tap in the middle of "sin(" moves to the nearer side of it, so a function is never split. */
    private fun snapOutOfFunction(expression: String, position: Int): Int {
        var i = 0
        while (i < expression.length) {
            val name = FUNCTION_NAMES.firstOrNull { expression.startsWith(it, i) }
            if (name == null) {
                i++
                continue
            }
            val end = i + name.length
            if (position > i && position < end) return if (position - i < end - position) i else end
            i = end
        }
        return position
    }

    fun toggleAngleMode() {
        val current = _uiState.value
        val switched = current.copy(degrees = !current.degrees)
        _uiState.value = switched.copy(result = evaluatePreview(current.expression, switched.degrees))
    }

    private val binaryOps = setOf("+", "-", "×", "÷", "^")

    private fun appendInput(expr: String, label: String): String {
        val last = expr.lastOrNull()?.toString()
        // After % or ) a new number means multiply: 5%2 → 5%×2, (2+3)4 → (2+3)×4
        // The same after π, e, ² and !
        val afterClosed = last == "%" || last == ")" || last == "π" || last == "e" || last == "²" || last == "!"
        return when {
            label in FUNCTION_KEYS || label == "π" || label == "e" -> {
                // Right after a value, a function or constant multiplies: 2π → 2×π, 3sin( → 3×sin(
                val text = FUNCTION_KEYS[label] ?: label
                val t = expr.trimEnd('.')
                val afterValue = t.lastOrNull()?.let { it.isDigit() || it in ")%πe²!" } == true
                if (afterValue) "$t×$text" else expr + text
            }
            label == "x²" || label == "!" -> {
                val t = expr.trimEnd('.')
                val afterValue = t.lastOrNull()?.let { it.isDigit() || it in ")%πe²!" } == true
                if (afterValue) t + (if (label == "x²") "²" else "!") else expr
            }
            label == "()" -> {
                // One smart key: closes an open bracket after a number, otherwise opens one.
                // A bracket right after a number multiplies: 2( → 2×(
                val stillOpen = expr.count { it == '(' } - expr.count { it == ')' }
                val c = expr.lastOrNull()
                val afterValue = c != null && (c.isDigit() || c in ").%πe²!")
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
        if (trailingOperator.containsMatchIn(expr)) return false
        // Editing in the middle can leave broken spots, like deleting the 1 in 45-1+124
        // gives 45-+124. Show no answer until it's fixed, instead of a wrong one.
        if (expr.first() in "+×÷^%)²!") return false
        for (i in 1 until expr.length) {
            val a = expr[i - 1]
            val b = expr[i]
            val aIsOp = a in "+-×÷^"
            when {
                // two signs in a row; only a minus after × ÷ ^ ( is fine (2×-3, (-3))
                b in "+×÷^" && (aIsOp || a == '(') -> return false
                b == '-' && a in "+-" -> return false
                // % or ) right after a sign or "("
                b in "%)²!" && (aIsOp || a == '(') -> return false
            }
        }
        return true
    }

    /** Adds the ")" still missing at the end, so (2+3 can already show 5. */
    private fun closeBrackets(expr: String): String {
        val stillOpen = expr.count { it == '(' } - expr.count { it == ')' }
        return if (stillOpen > 0) expr + ")".repeat(stillOpen) else expr
    }

    private fun evaluatePreview(expression: String, degrees: Boolean = _uiState.value.degrees): String {
        if (!isCompleteExpression(expression)) return ""
        return calculate(closeBrackets(expression), degrees) ?: ""
    }

    private fun evaluate() {
        val typed = _uiState.value.expression
        if (!isCompleteExpression(typed)) {
            _uiState.value = _uiState.value.copy(result = "Error")
            return
        }
        val expression = closeBrackets(typed)
        val resStr = calculate(expression, _uiState.value.degrees)
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
    private fun calculate(expression: String, degrees: Boolean): String? {
        ExactMath.evaluate(expression, degrees)?.let { return formatExact(it) }
        // Anything with sin, ln, π and so on that ExactMath refused is an error, not a job for exp4j
        if (hasScientificSymbols(expression)) return null
        return try {
            val sanitized = expression
                .replace("×", "*")
                .replace("÷", "/")
                .replace("%", "/100")

            val res = ExpressionBuilder(sanitized)
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