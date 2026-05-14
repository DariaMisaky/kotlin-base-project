package com.daria.kotlinbase.shared.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar

abstract class BaseFragment<B : ViewDataBinding, VM : BaseViewModel>(
    @LayoutRes private val layoutId: Int,
) : Fragment() {

    private var _binding: B? = null
    protected val binding: B get() = _binding!!

    protected abstract val viewModel: VM

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = DataBindingUtil.inflate(inflater, layoutId, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.baseCmd.observe(viewLifecycleOwner) { command ->
            handleBaseCommand(command)
        }
    }

    protected open fun handleBaseCommand(command: BaseCommand) {
        when (command) {
            is BaseCommand.PerformNavAction -> findNavController().navigate(command.navAction)
            is BaseCommand.GoBack -> findNavController().popBackStack()
            is BaseCommand.ShowSnackbar -> showSnackbar(command.message)
            is BaseCommand.ShowToast -> showToast(command.message)
            is BaseCommand.ShowError -> showSnackbar(command.message ?: getString(com.daria.kotlinbase.R.string.error_generic))
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
