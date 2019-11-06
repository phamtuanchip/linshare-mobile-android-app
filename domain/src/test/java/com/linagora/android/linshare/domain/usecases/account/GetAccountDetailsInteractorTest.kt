package com.linagora.android.linshare.domain.usecases.account

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.linagora.android.linshare.domain.network.manager.AuthorizationManager
import com.linagora.android.linshare.domain.repository.user.AuditUserRepository
import com.linagora.android.linshare.domain.usecases.utils.Failure
import com.linagora.android.linshare.domain.usecases.utils.State
import com.linagora.android.linshare.domain.usecases.utils.Success
import com.linagora.android.linshare.domain.utils.emitState
import com.linagora.android.testshared.TestFixtures.Accounts.LAST_LOGIN
import com.linagora.android.testshared.TestFixtures.Credentials.CREDENTIAL
import com.linagora.android.testshared.TestFixtures.Credentials.LINSHARE_CREDENTIAL
import com.linagora.android.testshared.TestFixtures.State.ACCOUNT_DETAILS_SUCCESS_STATE
import com.linagora.android.testshared.TestFixtures.State.AUTHENTICATE_SUCCESS_STATE
import com.linagora.android.testshared.TestFixtures.State.EMPTY_TOKEN_STATE
import com.linagora.android.testshared.TestFixtures.State.ERROR_STATE
import com.linagora.android.testshared.TestFixtures.State.INIT_STATE
import com.linagora.android.testshared.TestFixtures.State.LOADING_STATE
import com.linagora.android.testshared.TestFixtures.Tokens.TOKEN
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.MockitoAnnotations

class GetAccountDetailsInteractorTest {

    @Mock
    lateinit var getToken: GetTokenInteractor

    @Mock
    lateinit var authorizationManager: AuthorizationManager

    @Mock
    lateinit var auditUserRepository: AuditUserRepository

    private lateinit var getAccountDetails: GetAccountDetailsInteractor

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        getAccountDetails = GetAccountDetailsInteractor(getToken, authorizationManager, auditUserRepository)
    }

    @Test
    fun getAccountDetailsShouldSuccessWithRightCredential() {
        runBlockingTest {
            `when`(getToken(LINSHARE_CREDENTIAL))
                .then {
                    flow<State<Either<Failure, Success>>> {
                        emitState { LOADING_STATE }
                        emitState { AUTHENTICATE_SUCCESS_STATE }
                    }
                }

            `when`(auditUserRepository.getLastLogin())
                .thenAnswer { LAST_LOGIN }

            val states = getAccountDetails(LINSHARE_CREDENTIAL)
                .toList(ArrayList())

            assertThat(states).hasSize(3)
            assertThat(states[0](INIT_STATE))
                .isEqualTo(LOADING_STATE)
            assertThat(states[1](LOADING_STATE))
                .isEqualTo(AUTHENTICATE_SUCCESS_STATE)
            assertThat(states[2](AUTHENTICATE_SUCCESS_STATE))
                .isEqualTo(ACCOUNT_DETAILS_SUCCESS_STATE)

            verify(authorizationManager).updateToken(TOKEN)
        }
    }

    @Test
    fun getAccountDetailsShouldFailedWithWrongCredential() {
        runBlockingTest {
            `when`(getToken(CREDENTIAL))
                .then {
                    flow<State<Either<Failure, Success>>> {
                        emitState { LOADING_STATE }
                        emitState { EMPTY_TOKEN_STATE }
                    }
                }

            val states = getAccountDetails(CREDENTIAL)
                .toList(ArrayList())

            assertThat(states).hasSize(2)
            assertThat(states[0](INIT_STATE))
                .isEqualTo(LOADING_STATE)
            assertThat(states[1](LOADING_STATE))
                .isEqualTo(EMPTY_TOKEN_STATE)

            verifyNoInteractions(authorizationManager)
        }
    }

    @Test
    fun getAccountDetailsShouldErrorInLoadingLastLoginWhenLastLoginFailed() {
        runBlockingTest {
            `when`(getToken(LINSHARE_CREDENTIAL))
                .then {
                    flow<State<Either<Failure, Success>>> {
                        emitState { LOADING_STATE }
                        emitState { AUTHENTICATE_SUCCESS_STATE }
                    }
                }

            `when`(auditUserRepository.getLastLogin())
                .thenAnswer { null }

            val states = getAccountDetails(LINSHARE_CREDENTIAL)
                .toList(ArrayList())

            assertThat(states).hasSize(3)
            assertThat(states[0](INIT_STATE))
                .isEqualTo(LOADING_STATE)
            assertThat(states[1](LOADING_STATE))
                .isEqualTo(AUTHENTICATE_SUCCESS_STATE)
            assertThat(states[2](AUTHENTICATE_SUCCESS_STATE))
                .isEqualTo(ERROR_STATE)

            verify(authorizationManager).updateToken(TOKEN)
        }
    }
}
