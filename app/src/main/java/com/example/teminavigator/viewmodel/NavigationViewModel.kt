package com.example.teminavigator.viewmodel
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.example.teminavigator.ui.langs.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.core.content.edit
import androidx.lifecycle.viewModelScope
import com.example.teminavigator.domain.Destination
import com.example.teminavigator.domain.NavigationState
import com.example.teminavigator.domain.RobotController
import com.example.teminavigator.domain.RobotEvent
import kotlinx.coroutines.launch

class NavigationViewModel(
    app: Application,
    private val robot: RobotController
) : AndroidViewModel(app){
    // language setup
    private val prefs = app.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _language = MutableStateFlow(
        AppLanguage.fromCode(prefs.getString("language",null))
    )
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _navState = MutableStateFlow<NavigationState>(NavigationState.Idle)
    val navState : StateFlow<NavigationState> = _navState.asStateFlow()

    private val _readyState = MutableStateFlow(true)
    val readyState : StateFlow<Boolean> = _readyState.asStateFlow()

    private var currentDestination : Destination? = null
    private var _xFLow = MutableStateFlow(0.0)
    private var _yFLow = MutableStateFlow(0.0)

    var xFlow : StateFlow<Double> = _xFLow.asStateFlow()
    var yFlow : StateFlow<Double> = _yFLow.asStateFlow()

    fun setLanguage(language: AppLanguage){
        _language.value = language
        prefs.edit { putString("language", language.code) }
    }

    // robot integration
    init {
        robot.attach()
        viewModelScope.launch {
            robot.events.collect {event -> handleEvent(event)}
        }
    }

    fun startGuidance(destination: Destination){
        robot.speak("Guidance Placeholder $destination") //TODO: add to the language pack
        robot.goTo(destination.id)
    }
    fun cancelGuidance() = robot.stop()

    private fun handleEvent(event: RobotEvent){
        when (event){
            is RobotEvent.GoToCancelled -> _navState.value = NavigationState.Idle
            is RobotEvent.GoToFinished -> currentDestination?.let {_navState.value = NavigationState.Arrived(it)}
            is RobotEvent.GoToStarted -> currentDestination?.let { _navState.value =
                NavigationState.Guiding(it)
            }
            is RobotEvent.PositionChanged -> {
                _xFLow.value = event.x.toDouble()
                _yFLow.value = event.y.toDouble()
            }
            is RobotEvent.Ready -> _readyState.value = event.isReady
            is RobotEvent.SpeechRecognised -> TODO()
        }
    }

}