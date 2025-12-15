package com.example.aplicacionpaseador.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.aplicacionpaseador.data.AvailabilityRequest
import com.example.aplicacionpaseador.data.RetrofitClient
import com.example.aplicacionpaseador.data.UserInfo
import com.example.aplicacionpaseador.data.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val userPreferences = UserPreferences(application)
    private val _availability = MutableStateFlow<Boolean>(false)
    val availability: StateFlow<Boolean> = _availability

    private val _userInfo = MutableStateFlow<UserInfo?>(null)
    val userInfo: StateFlow<UserInfo?> = _userInfo

    init {
        loadUserInfo()
    }

    private fun loadUserInfo() {
        viewModelScope.launch {
            val token = userPreferences.accessToken.first()
            if (token != null) {
                try {
                    val response = RetrofitClient.instance.getMe("Bearer $token")
                    if (response.isSuccessful) {
                        _userInfo.value = response.body()
                    }
                } catch (_: Exception) { /* ignore */ }
            }
        }
    }

    fun toggleAvailability(isAvailable: Boolean) {
        viewModelScope.launch {
            try {
                val token = userPreferences.accessToken.first()
                if (token != null) {
                    val response = RetrofitClient.instance.setAvailability(
                        "Bearer $token",
                        AvailabilityRequest(isAvailable)
                    )
                    if (response.isSuccessful) {
                        _availability.value = isAvailable
                    }
                }
            } catch (_: Exception) {
                // Silenciar por ahora; se podría exponer error de red
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                userPreferences.clear()
                _userInfo.value = null
                _availability.value = false
            } catch (_: Exception) { }
        }
    }
}
