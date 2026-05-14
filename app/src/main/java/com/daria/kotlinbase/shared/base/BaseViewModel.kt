package com.daria.kotlinbase.shared.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daria.kotlinbase.shared.utils.LiveEvent
import com.daria.kotlinbase.shared.utils.MutableLiveEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

abstract class BaseViewModel : ViewModel() {

    protected val _baseCmd = MutableLiveEvent<BaseCommand>()
    val baseCmd: LiveEvent<BaseCommand> = _baseCmd

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    /**
     * Launch a coroutine that toggles the [isLoading] flag and surfaces any uncaught
     * Throwable as a [BaseCommand.ShowError]. Use it for API calls when you want a
     * loading indicator and consistent error reporting.
     */
    protected fun performApiCall(
        showLoading: Boolean = true,
        block: suspend CoroutineScope.() -> Unit,
    ): Job = viewModelScope.launch {
        try {
            if (showLoading) _isLoading.value = true
            block()
        } catch (e: Throwable) {
            _baseCmd.value = BaseCommand.ShowError(e.message)
        } finally {
            if (showLoading) _isLoading.value = false
        }
    }
}
