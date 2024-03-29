package com.kttelematictask.dependency_injection

import com.kttelematictask.viewmodels.UserViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module{
    viewModel{
        UserViewModel(userRepository = get())
    }
}