package com.kttelematictask.dependency_injection

import com.kttelematictask.repository.UserRepository
import org.koin.dsl.module

val repositoryModule = module {
    single {
        UserRepository()
    }
}