package com.voxeldev.canoe.projects.store

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.voxeldev.canoe.projects.api.ProjectsModel
import com.voxeldev.canoe.projects.api.ProjectsRequest
import com.voxeldev.canoe.projects.integration.GetProjectsUseCase
import com.voxeldev.canoe.projects.store.ProjectsStore.Intent
import com.voxeldev.canoe.projects.store.ProjectsStore.State

/**
 * @author nvoxel
 */
internal class ProjectsStoreProvider(
    private val storeFactory: StoreFactory,
    private val getProjectsUseCase: GetProjectsUseCase = GetProjectsUseCase(),
) {

    fun provide(): ProjectsStore =
        object :
            ProjectsStore,
            Store<Intent, State, Nothing> by storeFactory.create(
                name = STORE_NAME,
                initialState = State(),
                bootstrapper = SimpleBootstrapper(Unit),
                executorFactory = ::ExecutorImpl,
                reducer = ReducerImpl,
            ) {}

    private sealed class Msg {
        data class ProjectsLoaded(val projectsModel: ProjectsModel) : Msg()
        data object ProjectsLoading : Msg()
        data class Error(val message: String) : Msg()
        data class TextChanged(val text: String) : Msg()
        data class SearchToggled(val active: Boolean) : Msg()
    }

    private inner class ExecutorImpl : CoroutineExecutor<Intent, Unit, State, Msg, Nothing>() {
        override fun executeAction(action: Unit, getState: () -> State) =
            loadProjects(params = ProjectsRequest(searchQuery = getState().searchText))

        override fun executeIntent(intent: Intent, getState: () -> State) {
            when (intent) {
                is Intent.SetSearchText -> dispatch(message = Msg.TextChanged(text = intent.text))
                is Intent.ToggleSearch -> dispatch(message = Msg.SearchToggled(active = !getState().searchActive))
                is Intent.Search -> loadProjects(params = ProjectsRequest(searchQuery = getState().searchText))
            }
        }

        private fun loadProjects(params: ProjectsRequest) {
            dispatch(message = Msg.ProjectsLoading)
            getProjectsUseCase(params = params, scope = scope) { result ->
                result.fold(
                    onSuccess = { dispatch(message = Msg.ProjectsLoaded(projectsModel = it)) },
                    onFailure = { dispatch(message = Msg.Error(message = it.message ?: it.toString())) },
                )
            }
        }
    }

    private object ReducerImpl : Reducer<State, Msg> {
        override fun State.reduce(msg: Msg): State =
            when (msg) {
                is Msg.ProjectsLoaded -> copy(projectsModel = msg.projectsModel, errorText = null, isLoading = false)
                is Msg.ProjectsLoading -> copy(isLoading = true)
                is Msg.Error -> copy(errorText = msg.message, isLoading = false)
                is Msg.TextChanged -> copy(searchText = msg.text)
                is Msg.SearchToggled -> copy(searchActive = msg.active)
            }
    }

    private companion object {
        const val STORE_NAME = "ProjectsStore"
    }
}