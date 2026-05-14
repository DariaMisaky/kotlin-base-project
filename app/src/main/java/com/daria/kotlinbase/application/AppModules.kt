package com.daria.kotlinbase.application

import org.koin.dsl.module

object AppModules {
    private val placeholderModule = module { }
    val modules = listOf(placeholderModule)
}
