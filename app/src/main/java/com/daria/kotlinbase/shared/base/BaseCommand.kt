package com.daria.kotlinbase.shared.base

import androidx.navigation.NavDirections

sealed class BaseCommand {
    data class PerformNavAction(val navAction: NavDirections) : BaseCommand()

    data object GoBack : BaseCommand()

    data class ShowToast(val message: String) : BaseCommand()

    data class ShowSnackbar(val message: String) : BaseCommand()

    data class ShowError(val message: String?) : BaseCommand()
}
