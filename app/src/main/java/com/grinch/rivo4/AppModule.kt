package com.grinch.rivo4

import com.grinch.rivo4.controller.CallLogViewModel
import com.grinch.rivo4.controller.ContactsViewModel
import com.grinch.rivo4.modal.`interface`.ICallLogRepository
import com.grinch.rivo4.modal.`interface`.IContactsRepository
import com.grinch.rivo4.modal.repository.CallLogRepository
import com.grinch.rivo4.modal.repository.ContactsRepository
import com.grinch.rivo4.controller.util.PreferenceManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single<IContactsRepository> {
        ContactsRepository(androidContext().contentResolver)
    }
    single<ICallLogRepository> {
        CallLogRepository(androidContext().contentResolver)
    }
    single {
        PreferenceManager(androidContext())
    }
    viewModel { ContactsViewModel(get()) }
    viewModel { CallLogViewModel(get()) }
}