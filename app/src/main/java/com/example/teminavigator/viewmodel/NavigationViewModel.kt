package com.example.teminavigator.viewmodel
import android.app.Application
import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import com.example.teminavigator.ui.langs.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.core.content.edit
import androidx.lifecycle.viewModelScope
import com.example.teminavigator.data.CalibrationRepository
import com.example.teminavigator.domain.Destination
import com.example.teminavigator.domain.MapCalibration
import com.example.teminavigator.domain.NavigationState
import com.example.teminavigator.domain.RobotController
import com.example.teminavigator.domain.RobotEvent
import com.example.teminavigator.domain.RobotPose
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    private val _pose = MutableStateFlow<RobotPose?>(null)
    val pose: StateFlow<RobotPose?> = _pose.asStateFlow()

    private val calibrationRepo = CalibrationRepository(app)

    val calibration: StateFlow<MapCalibration> =
        calibrationRepo.calibration.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            MapCalibration(Offset.Zero, 50f, 0f)   // only used until the first load finishes
        )

    fun setCalibration(c: MapCalibration) {
        viewModelScope.launch { calibrationRepo.save(c) }
    }

    val destinations: StateFlow<List<Destination>> =
        combine(readyState, calibration) { ready, cal ->
            if (!ready) return@combine emptyList()

            val poses = withContext(Dispatchers.IO) { robot.locationPoses() }
            robot.availableLocations.map { name ->
                val pose = poses[name]
                Destination(
                    id = name,
                    displayName = name,   // TODO: registry
                    aliases = emptyList(), // TODO: alias registry
                    imagePx = pose?.let { (x, y, _) ->
                        cal.robotToImagePx(x.toFloat(), y.toFloat())
                    },
                    imageYawDeg = pose?.let { (_, _, yaw) ->
                        cal.arrowRotationDeg(yaw.toFloat())
                    },
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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

    fun toggleNavigationPause(destination: Destination) {
        _navState.value = when (_navState.value) {
            is NavigationState.Paused -> NavigationState.Guiding(destination)
            else -> NavigationState.Paused(destination)
        }
    }

    fun cancelGuidance() = robot.stop()

    fun speak(text:String){
        robot.speak(text)
    }

    private fun handleEvent(event: RobotEvent){
        when (event){
            is RobotEvent.GoToCancelled -> _navState.value = NavigationState.Idle
            is RobotEvent.GoToFinished -> currentDestination?.let {_navState.value = NavigationState.Arrived(it)}
            is RobotEvent.GoToStarted -> currentDestination?.let { _navState.value =
                NavigationState.Guiding(it)
            }
            is RobotEvent.PositionChanged -> {
                val (px, py) = calibration.value.robotToImagePx(event.x, event.y)
                _pose.value = RobotPose(px, py, calibration.value.arrowRotationDeg(event.yaw))
            }
            is RobotEvent.Ready -> _readyState.value = event.isReady
            is RobotEvent.SpeechRecognised -> TODO()
        }
    }

}