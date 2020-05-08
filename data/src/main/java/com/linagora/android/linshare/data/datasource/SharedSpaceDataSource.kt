package com.linagora.android.linshare.data.datasource

import com.linagora.android.linshare.domain.model.sharedspace.SharedSpaceNodeNested

interface SharedSpaceDataSource {

    suspend fun getSharedSpaces(): List<SharedSpaceNodeNested>
}