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

package com.linagora.android.linshare.view.sharedspacedocumentdestination

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import arrow.core.Either
import com.linagora.android.linshare.R
import com.linagora.android.linshare.databinding.FragmentSharedSpaceDocumentDestinationBinding
import com.linagora.android.linshare.domain.model.sharedspace.SharedSpaceId
import com.linagora.android.linshare.domain.model.sharedspace.WorkGroupDocument
import com.linagora.android.linshare.domain.model.sharedspace.WorkGroupNode
import com.linagora.android.linshare.domain.model.sharedspace.WorkGroupNodeId
import com.linagora.android.linshare.domain.usecases.sharedspace.GetSharedSpaceNodeSuccess
import com.linagora.android.linshare.domain.usecases.sharedspace.SharedSpaceDocumentItemClick
import com.linagora.android.linshare.domain.usecases.utils.Success
import com.linagora.android.linshare.model.parcelable.ParentDestinationInfo
import com.linagora.android.linshare.model.parcelable.SharedSpaceDestinationInfo
import com.linagora.android.linshare.model.parcelable.SharedSpaceNavigationInfo
import com.linagora.android.linshare.model.parcelable.UploadDestinationInfo
import com.linagora.android.linshare.model.parcelable.WorkGroupNodeIdParcelable
import com.linagora.android.linshare.model.parcelable.getParentNodeId
import com.linagora.android.linshare.model.parcelable.toParcelable
import com.linagora.android.linshare.model.parcelable.toSharedSpaceId
import com.linagora.android.linshare.model.parcelable.toWorkGroupNodeId
import com.linagora.android.linshare.util.getViewModel
import com.linagora.android.linshare.util.isRootFileType
import com.linagora.android.linshare.view.MainNavigationFragment
import com.linagora.android.linshare.view.Navigation
import com.linagora.android.linshare.view.Navigation.FileType
import com.linagora.android.linshare.view.upload.UploadFragment.Companion.UPLOAD_TO_MY_SPACE_DESTINATION_INFO
import org.slf4j.LoggerFactory

class SharedSpaceDocumentDestinationFragment : MainNavigationFragment() {

    private lateinit var viewModel: SharedSpaceDocumentDestinationViewModel

    private lateinit var binding: FragmentSharedSpaceDocumentDestinationBinding

    private val arguments: SharedSpaceDocumentDestinationFragmentArgs by navArgs()

    companion object {
        private val LOGGER = LoggerFactory.getLogger(SharedSpaceDocumentDestinationFragment::class.java)

        private const val ONLY_ROOT_ITEM = 1

        private val EMPTY_PARENT_NODE_ID = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSharedSpaceDocumentDestinationBinding.inflate(inflater, container, false)
        initViewModel(binding)
        return binding.root
    }

    override fun configureToolbar(toolbar: Toolbar) {
        toolbar.setNavigationIcon(R.drawable.ic_navigation_back)
        toolbar.navigationIcon?.setTint(ContextCompat.getColor(context!!, R.color.toolbar_primary_color))
        toolbar.setNavigationOnClickListener { navigateBack() }
    }

    private fun initViewModel(binding: FragmentSharedSpaceDocumentDestinationBinding) {
        viewModel = getViewModel(viewModelFactory)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        bindingNavigationInfo()
        observeViewState()
    }

    private fun bindingNavigationInfo() {
        binding.navigationInfo = arguments.navigationInfo
            ?.let { it }
            ?: arguments.uploadDestinationInfo
                ?.let { generateNavigationInfo(it) }
    }

    private fun generateNavigationInfo(uploadDestinationInfo: UploadDestinationInfo): SharedSpaceNavigationInfo {
        val fileType = generateFileTypeByUploadDestinationInfo(uploadDestinationInfo)
        return SharedSpaceNavigationInfo(
            uploadDestinationInfo.sharedSpaceDestinationInfo.sharedSpaceIdParcelable,
            fileType,
            getParentNodeIdFromUploadDestinationInfo(fileType, uploadDestinationInfo)
        )
    }

    private fun getParentNodeIdFromUploadDestinationInfo(fileType: FileType, uploadDestinationInfo: UploadDestinationInfo): WorkGroupNodeIdParcelable {
        return fileType.takeIf { it == FileType.NORMAL }
            ?.let { uploadDestinationInfo.parentDestinationInfo.parentNodeId }
            ?: WorkGroupNodeIdParcelable(uploadDestinationInfo.sharedSpaceDestinationInfo.sharedSpaceIdParcelable.uuid)
    }

    private fun generateFileTypeByUploadDestinationInfo(uploadDestinationInfo: UploadDestinationInfo): FileType {
        return takeIf { uploadDestinationInfo.isRootFileType() }
            ?.let { FileType.ROOT }
            ?: FileType.NORMAL
    }

    private fun observeViewState() {
        viewModel.viewState.observe(viewLifecycleOwner, Observer { state ->
            state.map { success ->
                when (success) {
                    is Success.ViewEvent -> reactToViewEvent(success)
                    is Success.ViewState -> reactToViewState(success)
                }
            }
        })
    }

    private fun reactToViewState(viewState: Success.ViewState) {
        bindingFolderName(viewState)
    }

    private fun reactToViewEvent(viewEvent: Success.ViewEvent) {
        when (viewEvent) {
            is SharedSpaceDocumentItemClick -> navigateIntoSubFolder(viewEvent.workGroupNode)
            is CancelPickDestinationViewState -> navigateToUpload(Navigation.UploadType.OUTSIDE_APP, arguments.uri, UPLOAD_TO_MY_SPACE_DESTINATION_INFO)
            is ChoosePickDestinationViewState -> handleChooseDestination()
        }

        viewModel.dispatchState(Either.right(Success.Idle))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpSwipeRefreshLayout()
        initData()
        setUpOnBackPressed()
    }

    private fun initData() {
        val sharedSpaceId = extractSharedSpaceId()
        val currentNodeId = extractCurrentNodeId()
        sharedSpaceId?.let {
            getAllNodes(it)
            getCurrentSharedSpace(it)
            currentNodeId?.let { workGroupNodeId ->
                getCurrentNode(it, workGroupNodeId)
            }
        }
    }

    private fun extractSharedSpaceId(): SharedSpaceId? {
        return arguments.navigationInfo
            ?.let { it.sharedSpaceIdParcelable.toSharedSpaceId() }
            ?: arguments.uploadDestinationInfo
                ?.let { it.sharedSpaceDestinationInfo.sharedSpaceIdParcelable.toSharedSpaceId() }
    }

    private fun extractCurrentNodeId(): WorkGroupNodeId? {
        return arguments.navigationInfo
            ?.let { it.nodeIdParcelable.toWorkGroupNodeId() }
            ?: arguments.uploadDestinationInfo
                ?.let { it.parentDestinationInfo.parentNodeId.toWorkGroupNodeId() }
    }

    private fun getCurrentNode(sharedSpaceId: SharedSpaceId, currentNodeId: WorkGroupNodeId) {
        viewModel.getCurrentNode(sharedSpaceId = sharedSpaceId, currentNodeId = currentNodeId)
    }

    private fun getCurrentSharedSpace(sharedSpaceId: SharedSpaceId) {
        viewModel.getCurrentSharedSpace(sharedSpaceId = sharedSpaceId)
    }

    private fun setUpSwipeRefreshLayout() {
        binding.swipeLayoutSharedSpace.setColorSchemeResources(R.color.colorPrimary)
    }

    private fun getAllNodes(sharedSpaceId: SharedSpaceId) {
        viewModel.getAllChildNodes(sharedSpaceId, getParentNodeId())
    }

    private fun getParentNodeId(): WorkGroupNodeId? {
        val navigationInfo = arguments.navigationInfo
        navigationInfo?.let { return getParentNodeIdFromNavigationInfo(it) }
            ?: return getParentNodeIdFromUploadDestination()
    }

    private fun getParentNodeIdFromNavigationInfo(navigationInfo: SharedSpaceNavigationInfo): WorkGroupNodeId? {
        return navigationInfo.getParentNodeId()
    }

    private fun getParentNodeIdFromUploadDestination(): WorkGroupNodeId? {
        val uploadDestinationInfo = arguments.uploadDestinationInfo
        return uploadDestinationInfo?.let { getParentNodeIdFromGenerateNavigationInfo(it) } ?: EMPTY_PARENT_NODE_ID
    }

    private fun getParentNodeIdFromGenerateNavigationInfo(uploadDestinationInfo: UploadDestinationInfo): WorkGroupNodeId? {
        return generateNavigationInfo(uploadDestinationInfo)?.getParentNodeId()
    }

    private fun bindingFolderName(viewState: Success.ViewState) {
        if (viewState is GetSharedSpaceNodeSuccess) {
            binding.navigationCurrentFolder.text = viewState.node.name
        }
    }

    private fun createUploadDestination(): UploadDestinationInfo {
        val currentSharedSpace = viewModel.currentSharedSpace.value
        val currentNode = viewModel.currentNode.value

        require(currentSharedSpace != null) { "sharedSpace is not available" }
        require(currentNode != null) { "workgroup node is not available" }

        return UploadDestinationInfo(
            sharedSpaceDestinationInfo = SharedSpaceDestinationInfo(
                currentSharedSpace.sharedSpaceId.toParcelable(),
                currentSharedSpace.name,
                currentSharedSpace.quotaId.toParcelable()
            ),
            parentDestinationInfo = ParentDestinationInfo(
                generateNodeIdByFileType(currentNode).toParcelable(),
                currentNode.name
            )
        )
    }

    private fun generateNodeIdByFileType(currentNode: WorkGroupNode): WorkGroupNodeId {
        return arguments.navigationInfo.takeIf { it?.fileType == FileType.ROOT }
            ?.let { currentNode.parentWorkGroupNodeId }
            ?: currentNode.workGroupNodeId
    }

    private fun handleChooseDestination() {
        runCatching { createUploadDestination() }
            .onFailure { LOGGER.error("handleChooseDestination(): ${it.printStackTrace()} - ${it.message}") }
            .map { navigateToUpload(Navigation.UploadType.OUTSIDE_APP_TO_WORKGROUP, arguments.uri, it) }
    }

    private fun navigateIntoSubFolder(workGroupNode: WorkGroupNode) {
        if (workGroupNode is WorkGroupDocument) {
            return
        }

        val action = SharedSpaceDocumentDestinationFragmentDirections.actionNavigationPickDestinationToPickDestination(
            arguments.uploadType,
            arguments.uri,
            arguments.uploadDestinationInfo,
            generateNavigationInfoForSubFolder(workGroupNode))
        findNavController().navigate(action)
    }

    private fun generateNavigationInfoForSubFolder(workGroupNode: WorkGroupNode): SharedSpaceNavigationInfo {
        return SharedSpaceNavigationInfo(
            sharedSpaceIdParcelable = workGroupNode.sharedSpaceId.toParcelable(),
            fileType = FileType.NORMAL,
            nodeIdParcelable = WorkGroupNodeIdParcelable(workGroupNode.workGroupNodeId.uuid)
        )
    }

    private fun generateNavigationInfoForPreviousFolder(workGroupNode: WorkGroupNode): SharedSpaceNavigationInfo {
        val lastTreePath = workGroupNode.treePath.last()
        val destinationFileType = workGroupNode.treePath.takeIf { it.size == ONLY_ROOT_ITEM }
            ?.let { FileType.ROOT }
            ?: FileType.NORMAL
        return SharedSpaceNavigationInfo(
            sharedSpaceIdParcelable = workGroupNode.sharedSpaceId.toParcelable(),
            fileType = destinationFileType,
            nodeIdParcelable = WorkGroupNodeIdParcelable(lastTreePath.workGroupNodeId.uuid)
        )
    }

    private fun navigateBack() {
        viewModel.currentNode.value?.let { node ->
            node.treePath.takeIf { it.isNullOrEmpty() }
                ?.let { navigateToWorkGroup() }
                ?: navigateToPreviousFolder(node)
        }
    }

    private fun setUpOnBackPressed() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) { navigateBack() }
    }

    private fun navigateToUpload(uploadType: Navigation.UploadType, uri: Uri, uploadDestinationInfo: UploadDestinationInfo?) {
        val action = SharedSpaceDocumentDestinationFragmentDirections.actionNavigationPickDestinationToUploadFragment(uploadType, uri, uploadDestinationInfo)
        findNavController().navigate(action)
    }

    private fun navigateToPreviousFolder(workGroupNode: WorkGroupNode) {
        val action = SharedSpaceDocumentDestinationFragmentDirections.actionNavigationPickDestinationToPickDestination(
            arguments.uploadType,
            arguments.uri,
            arguments.uploadDestinationInfo,
            generateNavigationInfoForPreviousFolder(workGroupNode))
        findNavController().navigate(action)
    }

    private fun navigateToWorkGroup() {
        val action = SharedSpaceDocumentDestinationFragmentDirections.actionNavigationPickDestinationToNavigationDestination(
            arguments.uploadType,
            arguments.uri,
            arguments.uploadDestinationInfo)
        findNavController().navigate(action)
    }
}
