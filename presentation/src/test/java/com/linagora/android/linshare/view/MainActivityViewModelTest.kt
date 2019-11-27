package com.linagora.android.linshare.view

import androidx.lifecycle.Observer
import arrow.core.Either
import com.linagora.android.linshare.CoroutinesExtension
import com.linagora.android.linshare.InstantExecutorExtension
import com.linagora.android.linshare.domain.network.manager.AuthorizationManager
import com.linagora.android.linshare.domain.usecases.auth.GetAuthenticatedInfoInteractor
import com.linagora.android.linshare.domain.usecases.utils.Failure
import com.linagora.android.linshare.domain.usecases.utils.State
import com.linagora.android.linshare.domain.usecases.utils.Success
import com.linagora.android.linshare.domain.utils.emitState
import com.linagora.android.linshare.network.DynamicBaseUrlInterceptor
import com.linagora.android.linshare.runBlockingTest
import com.linagora.android.linshare.utils.provideFakeCoroutinesDispatcherProvider
import com.linagora.android.testshared.TestFixtures
import kotlinx.coroutines.flow.flow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

@ExtendWith(InstantExecutorExtension::class)
class MainActivityViewModelTest {

    companion object {
        @JvmField
        @RegisterExtension
        val coroutinesExtension = CoroutinesExtension()
    }

    @Mock
    lateinit var getAuthenticatedInfoInteractor: GetAuthenticatedInfoInteractor

    @Mock
    lateinit var dynamicBaseUrlInterceptor: DynamicBaseUrlInterceptor

    @Mock
    lateinit var authorizationManager: AuthorizationManager

    @Mock
    lateinit var viewObserver: Observer<Either<Failure, Success>>

    private lateinit var viewModel: MainActivityViewModel

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        viewModel =
            MainActivityViewModel(
                getAuthenticatedInfo = getAuthenticatedInfoInteractor,
                dispatcherProvider = provideFakeCoroutinesDispatcherProvider(coroutinesExtension.testDispatcher),
                dynamicBaseUrlInterceptor = dynamicBaseUrlInterceptor,
                authorizationManager = authorizationManager
            )
    }

    @Test
    fun checkSignedInShouldProduceSuccessState() {
        coroutinesExtension.runBlockingTest {
            Mockito.`when`(getAuthenticatedInfoInteractor())
                .thenAnswer {
                    flow<State<Either<Failure, Success>>> {
                        emitState { TestFixtures.State.LOADING_STATE }
                        emitState { TestFixtures.State.AUTHENTICATE_SUCCESS_STATE }
                    }
                }

            viewModel.viewState.observeForever(viewObserver)

            viewModel.checkSignedIn()
            Mockito.verify(viewObserver).onChanged(TestFixtures.State.LOADING_STATE)
            Mockito.verify(viewObserver).onChanged(TestFixtures.State.AUTHENTICATE_SUCCESS_STATE)
        }
    }

    @Test
    fun checkSignedInShouldProduceWrongCredentialStateWhenCredentialNotExist() {
        coroutinesExtension.runBlockingTest {
            Mockito.`when`(getAuthenticatedInfoInteractor())
                .thenAnswer {
                    flow<State<Either<Failure, Success>>> {
                        emitState { TestFixtures.State.WRONG_CREDENTIAL_STATE }
                    }
                }

            viewModel.viewState.observeForever(viewObserver)

            viewModel.checkSignedIn()
            Mockito.verify(viewObserver).onChanged(TestFixtures.State.WRONG_CREDENTIAL_STATE)
        }
    }

    @Test
    fun checkSignedInShouldProduceEmptyTokenStateWhenTokenNotExist() {
        coroutinesExtension.runBlockingTest {
            Mockito.`when`(getAuthenticatedInfoInteractor())
                .thenAnswer {
                    flow<State<Either<Failure, Success>>> {
                        emitState { TestFixtures.State.LOADING_STATE }
                        emitState { TestFixtures.State.EMPTY_TOKEN_STATE }
                    }
                }

            viewModel.viewState.observeForever(viewObserver)

            viewModel.checkSignedIn()
            Mockito.verify(viewObserver).onChanged(TestFixtures.State.LOADING_STATE)
            Mockito.verify(viewObserver).onChanged(TestFixtures.State.EMPTY_TOKEN_STATE)
        }
    }
}