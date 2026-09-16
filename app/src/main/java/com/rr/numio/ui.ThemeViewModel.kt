package com.rr.numio.ui

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val android.content.Context.dataStore: DataStore<Preferences>
        by preferencesDataStore(name = "numio_prefs")

private val ACCENT_COLOR_KEY = stringPreferencesKey("accent_color")
private const val DEFAULT_COLOR = "FFD23F"

class ThemeViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStore = application.dataStore

    private val _accentColor = MutableStateFlow(DEFAULT_COLOR)
    val accentColor: StateFlow<String> = _accentColor

    init {
        viewModelScope.launch {
            val prefs = dataStore.data.first()
            _accentColor.value = prefs[ACCENT_COLOR_KEY] ?: DEFAULT_COLOR
        }
    }

    fun setAccentColor(hex: String) {
        val clean = hex.trimStart('#').uppercase()
        if (clean.length == 6 && clean.all { it.isDigit() || it in 'A'..'F' }) {
            _accentColor.value = clean
            viewModelScope.launch {
                dataStore.edit { prefs ->
                    prefs[ACCENT_COLOR_KEY] = clean
                }
            }
        }
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ThemeViewModel(application) as T
                }
            }
    }
}