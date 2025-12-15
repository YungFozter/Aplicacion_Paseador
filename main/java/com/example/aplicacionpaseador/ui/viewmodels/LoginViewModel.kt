package com.example.aplicacionpaseador.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.aplicacionpaseador.data.LoginRequest
import com.example.aplicacionpaseador.data.RetrofitClient
import com.example.aplicacionpaseador.data.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val userPreferences = UserPreferences(application)
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val response = RetrofitClient.instance.login(LoginRequest(email, password))
                if (response.isSuccessful && response.body() != null) {
                    userPreferences.saveAccessToken(response.body()!!.accessToken)
                    _loginState.value = LoginState.Success
                } else {
                    _loginState.value = LoginState.Error("Email o contraseña incorrectos.")
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error("Error de red. Inténtalo de nuevo.")
            }
        }
    }

    sealed class LoginState {
        object Idle : LoginState()
        object Loading : LoginState()
        object Success : LoginState()
        data class Error(val message: String) : LoginState()
    }
}

