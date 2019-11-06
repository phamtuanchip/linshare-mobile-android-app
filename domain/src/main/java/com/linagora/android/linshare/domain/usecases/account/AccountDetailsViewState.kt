package com.linagora.android.linshare.domain.usecases.account

import com.linagora.android.linshare.domain.model.Credential
import com.linagora.android.linshare.domain.model.LastLogin
import com.linagora.android.linshare.domain.model.Token
import com.linagora.android.linshare.domain.usecases.utils.Success

data class AccountDetailsViewState(
    val isLoading: Boolean = false,
    val credential: Credential? = null,
    val token: Token? = null,
    val lastLogin: LastLogin? = null
) : Success.ViewState() {

    companion object {
        val INIT_STATE = AccountDetailsViewState(isLoading = true)
    }
}