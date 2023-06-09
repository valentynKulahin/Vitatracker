package app.mybad.notifier.ui.screens.mycourses

import androidx.lifecycle.ViewModel
import app.mybad.domain.repos.CoursesRepo
import app.mybad.domain.repos.UsagesRepo
import app.mybad.domain.usecases.courses.DeleteCourseUseCase
import app.mybad.domain.usecases.courses.LoadCoursesUseCase
import app.mybad.domain.usecases.courses.UpdateCourseUseCase
import app.mybad.domain.usecases.meds.UpdateMedUseCase
import app.mybad.domain.usecases.usages.UpdateAllUsagesInCourseUseCase
import app.mybad.network.repos.repo.CoursesNetworkRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyCoursesViewModel @Inject constructor(
    private val loadCourses: LoadCoursesUseCase,
    private val deleteCourse: DeleteCourseUseCase,
    private val updateCourse: UpdateCourseUseCase,
    private val updateMed: UpdateMedUseCase,
    private val coursesRepo: CoursesRepo,
    private val usagesRepo: UsagesRepo,
    private val updateUsagesInCourse: UpdateAllUsagesInCourseUseCase,
    private val coursesNetworkRepo: CoursesNetworkRepo
) : ViewModel() {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val _state = MutableStateFlow(MyCoursesState())
    val state get() = _state.asStateFlow()

    init {
        scope.launch {
            loadCourses.getCoursesFlow().collect { courses ->
                _state.update {
                    it.copy(courses = courses)
                }
            }
        }
        scope.launch {
            loadCourses.getMedsFlow().collect { meds -> _state.update { it.copy(meds = meds) } }
        }
        scope.launch {
            loadCourses.getUsagesFlow().collect { usages ->
                _state.update {
                    it.copy(usages = usages)
                }
            }
        }
    }

    fun reduce(intent: MyCoursesIntent) {
        when (intent) {
            is MyCoursesIntent.Delete -> {
                scope.launch {
                    val mId = coursesRepo.getSingle(intent.courseId).medId
                    deleteCourse.execute(intent.courseId)
                    coursesNetworkRepo.deleteMed(mId)
                }
            }
            is MyCoursesIntent.Update -> {
                scope.launch {
                    updateMed(intent.med)
                    updateCourse.execute(intent.course.id, intent.course)
                    updateUsagesInCourse(intent.usagesPattern, intent.med, intent.course)
                    coursesNetworkRepo.updateAll(
                        med = intent.med,
                        course = intent.course,
                        usages = usagesRepo.getUsagesByMedId(intent.med.id)
                    )
                }
            }
        }
    }
}
