/*
 * LinShare is an open source filesharing software, part of the LinPKI software
 * suite, developed by Linagora.
 *
 * Copyright (C) 2020 LINAGORA
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version,
 * provided you comply with the Additional Terms applicable for LinShare software by
 * Linagora pursuant to Section 7 of the GNU Affero General Public License,
 * subsections (b), (c), and (e), pursuant to which you must notably (i) retain the
 * display in the interface of the “LinShare™” trademark/logo, the "Libre & Free" mention,
 * the words “You are using the Free and Open Source version of LinShare™, powered by
 * Linagora © 2009–2020. Contribute to Linshare R&D by subscribing to an Enterprise
 * offer!”. You must also retain the latter notice in all asynchronous messages such as
 * e-mails sent with the Program, (ii) retain all hypertext links between LinShare and
 * http://www.linshare.org, between linagora.com and Linagora, and (iii) refrain from
 * infringing Linagora intellectual property rights over its trademarks and commercial
 * brands. Other Additional Terms apply, see
 * <http://www.linshare.org/licenses/LinShare-License_AfferoGPL-v3.pdf>
 * for more details.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for
 * more details.
 * You should have received a copy of the GNU Affero General Public License and its
 * applicable Additional Terms for LinShare along with this program. If not, see
 * <http://www.gnu.org/licenses/> for the GNU Affero General Public License version
 *  3 and <http://www.linshare.org/licenses/LinShare-License_AfferoGPL-v3.pdf> for
 *  the Additional Terms applicable to LinShare software.
 */

package com.linagora.android.linshare.domain.usecases.autocomplete

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.linagora.android.linshare.domain.model.autocomplete.AutoCompletePattern
import com.linagora.android.linshare.domain.model.autocomplete.AutoCompleteType
import com.linagora.android.linshare.domain.model.autocomplete.UserAutoCompleteResult
import com.linagora.android.linshare.domain.model.sharedspace.SharedSpaceId
import com.linagora.android.linshare.domain.repository.autocomplete.AutoCompleteRepository
import com.linagora.android.testshared.AutoCompleteFixtures.NO_RESULT_USER_AUTOCOMPLETE_STATE
import com.linagora.android.testshared.AutoCompleteFixtures.THREAD_MEMBER_AUTO_COMPLETE_RESULT_1
import com.linagora.android.testshared.AutoCompleteFixtures.THREAD_MEMBER_AUTO_COMPLETE_RESULT_2
import com.linagora.android.testshared.AutoCompleteFixtures.THREAD_MEMBER_AUTO_COMPLETE_STATE
import com.linagora.android.testshared.AutoCompleteFixtures.USER_AUTOCOMPLETE_1
import com.linagora.android.testshared.AutoCompleteFixtures.USER_AUTOCOMPLETE_2
import com.linagora.android.testshared.AutoCompleteFixtures.USER_AUTOCOMPLETE_STATE
import com.linagora.android.testshared.TestFixtures.State.LOADING_STATE
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import java.util.UUID

class GetAutoCompleteSharingInteractorTest {

    @Mock
    lateinit var autoCompleteRepository: AutoCompleteRepository

    private lateinit var autoCompleteSharingInteractor: GetAutoCompleteSharingInteractor

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        autoCompleteSharingInteractor = GetAutoCompleteSharingInteractor(autoCompleteRepository)
    }

    @Test
    fun getAutoCompleteSharingShouldReturnResultWhenPatternMatched() = runBlockingTest {
        `when`(autoCompleteRepository.getAutoComplete(AutoCompletePattern("user"), AutoCompleteType.SHARING))
            .thenAnswer { listOf(USER_AUTOCOMPLETE_1, USER_AUTOCOMPLETE_2) }

        assertThat(autoCompleteSharingInteractor(AutoCompletePattern("user"), AutoCompleteType.SHARING)
                .map { it }
                .toList(ArrayList()))
            .containsExactly(LOADING_STATE, USER_AUTOCOMPLETE_STATE)
    }

    @Test
    fun getAutoCompleteSharingShouldReturnEmptyWhenPatternUnMatched() = runBlockingTest {
        `when`(autoCompleteRepository.getAutoComplete(AutoCompletePattern("invalid"), AutoCompleteType.SHARING))
            .thenAnswer { emptyList<UserAutoCompleteResult>() }

        assertThat(autoCompleteSharingInteractor(AutoCompletePattern("invalid"), AutoCompleteType.SHARING)
                .map { it }
                .toList(ArrayList()))
            .containsExactly(LOADING_STATE, NO_RESULT_USER_AUTOCOMPLETE_STATE)
    }

    @Test
    fun getAutoCompleteSharingShouldReturnThreadMemberResultWhenPatternMatched() = runBlockingTest {
        val threadUuid = SharedSpaceId(UUID.fromString("49ac407c-df87-49b1-b961-90d7eafe4217"))
        `when`(autoCompleteRepository.getAutoComplete(AutoCompletePattern("user"), AutoCompleteType.THREAD_MEMBERS, threadUuid))
            .thenAnswer { listOf(THREAD_MEMBER_AUTO_COMPLETE_RESULT_1, THREAD_MEMBER_AUTO_COMPLETE_RESULT_2) }

        assertThat(autoCompleteSharingInteractor(AutoCompletePattern("user"), AutoCompleteType.THREAD_MEMBERS, threadUuid)
                .map { it }
                .toList(ArrayList()))
            .containsExactly(LOADING_STATE, THREAD_MEMBER_AUTO_COMPLETE_STATE)
    }

    @Test
    fun getAutoCompleteSharingShouldReturnEmptyThreadMemberWhenPatternUnMatched() = runBlockingTest {
        val threadUuid = SharedSpaceId(UUID.fromString("49ac407c-df87-49b1-b961-90d7eafe4217"))
        `when`(autoCompleteRepository.getAutoComplete(AutoCompletePattern("invalid"), AutoCompleteType.THREAD_MEMBERS, threadUuid))
            .thenAnswer { emptyList<UserAutoCompleteResult>() }

        assertThat(autoCompleteSharingInteractor(AutoCompletePattern("invalid"), AutoCompleteType.THREAD_MEMBERS, threadUuid)
                .map { it }
                .toList(ArrayList()))
            .containsExactly(LOADING_STATE, NO_RESULT_USER_AUTOCOMPLETE_STATE)
    }

    @Test
    fun getAutoCompleteSharingShouldThrowWhenGetThreadMemberAutoCompleteButNoProvideThreadUUID() = runBlockingTest {
        val exception = IllegalArgumentException()
        `when`(autoCompleteRepository.getAutoComplete(AutoCompletePattern("user"), AutoCompleteType.THREAD_MEMBERS))
            .thenThrow(exception)

        assertThat(autoCompleteSharingInteractor(AutoCompletePattern("user"), AutoCompleteType.THREAD_MEMBERS)
                .map { it }
                .toList(ArrayList()))
            .containsExactly(LOADING_STATE, Either.left(AutoCompleteFailure(exception)))
    }
}
