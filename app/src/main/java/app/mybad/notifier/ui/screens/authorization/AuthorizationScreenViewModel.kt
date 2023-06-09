package app.mybad.notifier.ui.screens.authorization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.mybad.domain.repos.AuthorizationRepo
import app.mybad.domain.repos.DataStoreRepo
import app.mybad.domain.utils.ApiResult
import app.mybad.network.models.response.Authorization
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthorizationScreenViewModel @Inject constructor(
    private val authorizationRepo: AuthorizationRepo,
    private val dataStoreRepo: DataStoreRepo
) : ViewModel() {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val _uiState = MutableStateFlow(AuthorizationScreenContract())
    val uiState = _uiState.asStateFlow()

    suspend fun logIn(login: String, password: String) {
        val result = authorizationRepo.loginWithEmail(login = login, password = password)
        viewModelScope.launch {
            when (result) {
                is ApiResult.ApiSuccess -> setResultData(result.data as Authorization)
                is ApiResult.ApiError -> _uiState.emit(_uiState.value.copy(error = "${result.code} ${result.message}"))
                is ApiResult.ApiException -> _uiState.emit(_uiState.value.copy(exception = "${result.e}"))
            }
        }
    }

    private suspend fun setResultData(result: Authorization) {
        dataStoreRepo.updateToken(token = result.token)
        dataStoreRepo.updateUserId(userId = result.userId)
    }

    suspend fun registration(login: String, password: String, userName: String) {
        val result = authorizationRepo.registrationUser(
            login = login,
            password = password,
            userName = userName
        )
        viewModelScope.launch {
            when (result) {
                is ApiResult.ApiSuccess -> setResultData(result.data as Authorization)
                is ApiResult.ApiError -> _uiState.emit(_uiState.value.copy(error = "${result.code} ${result.message}"))
                is ApiResult.ApiException -> _uiState.emit(_uiState.value.copy(exception = "${result.e}"))
            }
        }
    }

    fun loginWithFacebook() {
        scope.launch { authorizationRepo.loginWithFacebook() }
    }

    fun loginWithGoogle() {
        scope.launch { authorizationRepo.loginWithGoogle() }
    }
}
