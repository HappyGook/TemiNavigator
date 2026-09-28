package com.example.teminavigator.viewmodel
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.example.teminavigator.ui.langs.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.core.content.edit

class NavigationViewModel(app: Application) : AndroidViewModel(app){
    // language setup
    private val prefs = app.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _language = MutableStateFlow(
        AppLanguage.fromCode(prefs.getString("language",null))
    )
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    fun setLanguage(language: AppLanguage){
        _language.value = language
        prefs.edit { putString("language", language.code) }
    }

}