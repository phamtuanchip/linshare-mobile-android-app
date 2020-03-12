package com.linagora.android.linshare.view.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.linagora.android.linshare.databinding.FragmentMainBinding
import com.linagora.android.linshare.domain.usecases.auth.AuthenticationViewState
import com.linagora.android.linshare.view.MainActivityViewModel
import com.linagora.android.linshare.view.MainNavigationFragment
import com.linagora.android.linshare.view.Navigation.LoginFlow
import org.slf4j.LoggerFactory
import javax.inject.Inject

class MainFragment : MainNavigationFragment() {

    companion object {
        private val LOGGER = LoggerFactory.getLogger(MainFragment::class.java)
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val mainActivityViewModel: MainActivityViewModel
            by activityViewModels { viewModelFactory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentMainBinding.inflate(inflater, container, false)
        initViewModel()
        return binding.root
    }

    private fun initViewModel() {
        mainActivityViewModel.viewState.observe(viewLifecycleOwner, Observer { state ->
            state.fold(
                ifLeft = { gotoLoginPage() },
                ifRight = { success ->
                    success.takeIf { it is AuthenticationViewState }
                        ?.let { jumpIn() }
                }
            )
        })
    }

    private fun gotoLoginPage() {
        LOGGER.info("gotoLoginPage()")
        val action = MainFragmentDirections.actionMainFragmentToWizardFragment(LoginFlow.DIRECT)
        findNavController().navigate(action)
    }

    private fun jumpIn() {
        LOGGER.info("jumpIn()")
        val action = MainFragmentDirections
            .actionMainFragmentToMySpaceFragment()
        findNavController().navigate(action)
    }
}
