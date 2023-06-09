package app.mybad.network.repos.impl

import android.util.Log
import app.mybad.domain.models.course.CourseDomainModel
import app.mybad.domain.models.med.MedDomainModel
import app.mybad.domain.models.usages.UsageCommonDomainModel
import app.mybad.domain.repos.CoursesRepo
import app.mybad.domain.repos.DataStoreRepo
import app.mybad.domain.repos.MedsRepo
import app.mybad.domain.repos.UsagesRepo
import app.mybad.domain.utils.ApiResult
import app.mybad.network.api.CoursesApi
import app.mybad.network.models.mapToDomain
import app.mybad.network.models.mapToNet
import app.mybad.network.models.response.Remedies
import app.mybad.network.models.UserModel
import app.mybad.network.repos.repo.CoursesNetworkRepo
import app.mybad.network.utils.ApiHandler.handleApi
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.Call
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
@Singleton
class CoursesNetworkRepoImpl @Inject constructor(
    private val coursesApi: CoursesApi,
    private val dataStoreRepo: DataStoreRepo,
    private val coursesRepo: CoursesRepo,
    private val usagesRepo: UsagesRepo,
    private val medsRepo: MedsRepo,
) : CoursesNetworkRepo {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var token = ""
    private var userId: Long? = null
    private val _result = MutableStateFlow<ApiResult>(ApiResult.ApiSuccess(""))
    override val result: StateFlow<ApiResult> get() = _result
    init {
        scope.launch {
            dataStoreRepo.getToken().collect {
                token = it
                if (token.isNotBlank()) {
                    val b2 = token.split('.')[1]
                    val body = Base64.UrlSafe.decode(b2).decodeToString()
                    val gson = Gson()
                    userId = gson.fromJson(body, Map::class.java)["id"].toString().toLongOrNull()
                }
            }
        }
    }
    override suspend fun getUserModel() {
        try {
            if (userId != null) {
                val r = handleApi { coursesApi.getUserModel(userId!!).execute() }
                if (r is ApiResult.ApiSuccess && r.data is UserModel) {
                    (r.data as UserModel).remedies?.forEach { remedies ->
                        medsRepo.add(remedies.mapToDomain())
                        remedies.courses?.forEach { courses ->
                            coursesRepo.add(courses.mapToDomain(remedies.userId))
                            courses.usages?.mapToDomain(courses.remedyId, remedies.userId)?.let {
                                usagesRepo.addUsages(it)
                            }
                        }
                    }
                }
            } else {
                ApiResult.ApiError(666, "null user id")
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    override suspend fun getAll() {
        try {
            if (userId != null) {
                val r = handleApi { coursesApi.getAll().execute() }
                Log.w("CNRI", "api result: $r")
                if (
                    r is ApiResult.ApiSuccess &&
                    r.data is List<*> &&
                    (r.data as List<*>).isNotEmpty() &&
                    (r.data as List<*>).first() is Remedies
                ) {
                    @Suppress("UNCHECKED_CAST")
                    (r.data as List<Remedies>).forEach { remedies ->
                        Log.w("CNRI", "remedy: $remedies")
                        medsRepo.add(remedies.mapToDomain())
                        remedies.courses?.forEach { courses ->
                            Log.w("CNRI", "course: $courses")
                            coursesRepo.add(courses.mapToDomain(userId!!))
                            courses.usages?.mapToDomain(courses.remedyId, userId!!)?.let {
                                usagesRepo.addUsages(it)
                            }
                        }
                    }
                }
            } else {
                ApiResult.ApiError(666, "null user id")
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }
    override suspend fun updateUsage(usage: UsageCommonDomainModel) {
        val a = coursesRepo.getAll().first { it.medId == usage.medId }
        val u = usage.mapToNet(a.id)
        execute { coursesApi.updateUsage(u, u.id) }
    }

    override suspend fun updateAll(
        med: MedDomainModel,
        course: CourseDomainModel,
        usages: List<UsageCommonDomainModel>
    ) {
        Log.w("CNRI_update", "updating ${med.name} #${course.id}")
        val courses = course.mapToNet(usages)
        val remedies = Remedies(
            id = med.id,
            userId = userId!!,
            name = med.name.toString(),
            description = med.description.toString(),
            comment = med.comment.toString(),
            type = med.type,
            icon = med.icon.toLong(),
            color = med.color.toLong(),
            dose = med.dose.toLong(),
            measureUnit = med.measureUnit,
            beforeFood = med.beforeFood,
            photo = "",
            historyRemedys = emptyList(),
            courses = listOf(courses)
        )
        execute { coursesApi.addAll(remedies) }
    }

    override suspend fun deleteMed(medId: Long) {
        execute { coursesApi.deleteMed(medId) }
    }

    private suspend fun execute(request: () -> Call<*>): ApiResult {
        return when (val response = handleApi { request.invoke().execute() }) {
            is ApiResult.ApiSuccess -> ApiResult.ApiSuccess(data = response.data)
            is ApiResult.ApiError -> ApiResult.ApiError(
                code = response.code,
                message = response.message
            )
            is ApiResult.ApiException -> ApiResult.ApiException(e = response.e)
        }
    }
}
