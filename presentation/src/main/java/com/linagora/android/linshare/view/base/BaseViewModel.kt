package com.linagora.android.linshare.view.base

import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import arrow.core.Either
import com.linagora.android.linshare.domain.usecases.utils.Failure
import com.linagora.android.linshare.domain.usecases.utils.State
import com.linagora.android.linshare.domain.usecases.utils.Success
import com.linagora.android.linshare.util.CoroutinesDispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext

abstract class BaseViewModel(
    private val dispatcherProvider: CoroutinesDispatcherProvider
) : ViewModel() {

    companion object {
        private val INITIAL_STATE = Either.Right(Success.Idle)
    }

    private val state = MutableLiveData<Either<Failure, Success>>()
        .apply { value = INITIAL_STATE }

    val viewState: LiveData<Either<Failure, Success>> = state

    @MainThread
    fun dispatchState(state: Either<Failure, Success>) {
        this.state.value = state
    }

    suspend fun consumeStates(states: Flow<State<Either<Failure, Success>>>) {
        states.collect {
            withContext(dispatcherProvider.main) {
                dispatchState(it(state()))
            }
        }
    }

    private fun state() = state.value!!
}