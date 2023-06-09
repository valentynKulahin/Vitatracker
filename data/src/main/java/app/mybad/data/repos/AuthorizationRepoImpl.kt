package app.mybad.data.repos

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import app.mybad.domain.repos.AuthorizationRepo
import app.mybad.domain.utils.ApiResult
import app.mybad.network.models.request.AuthorizationUserLogin
import app.mybad.network.models.request.AuthorizationUserRegistration
import app.mybad.network.repos.repo.AuthorizationNetworkRepo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthorizationRepoImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val authorizationNetworkRepo: AuthorizationNetworkRepo
) : AuthorizationRepo {

    override suspend fun loginWithFacebook() {
        TODO("Not yet implemented")
    }

    override suspend fun loginWithGoogle() {
        TODO("Not yet implemented")
    }

    override suspend fun loginWithEmail(login: String, password: String): ApiResult {
        return authorizationNetworkRepo.loginUser(
            authorizationUserLogin = AuthorizationUserLogin(
                email = login,
                password = password
            )
        )
    }

    override suspend fun registrationUser(
        login: String,
        password: String,
        userName: String
    ): ApiResult {
        return authorizationNetworkRepo.registrationUser(
            authorizationUserRegistration = AuthorizationUserRegistration(
                email = login,
                password = password,
                name = userName
            )
        )
    }
}
