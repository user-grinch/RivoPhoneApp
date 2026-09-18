package com.grinch.rivo4

import androidx.room.Room
import com.grinch.rivo4.modal.db.RivoDatabase
import com.grinch.rivo4.controller.BackupViewModel
import com.grinch.rivo4.controller.CallLogViewModel
import com.grinch.rivo4.controller.ContactsViewModel
import com.grinch.rivo4.modal.`interface`.ICallLogRepository
import com.grinch.rivo4.modal.`interface`.IContactsRepository
import com.grinch.rivo4.modal.repository.CallLogRepository
import com.grinch.rivo4.modal.repository.ContactsRepository
import com.grinch.rivo4.controller.CallAnalyticsViewModel
import com.grinch.rivo4.controller.util.PreferenceManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            RivoDatabase::class.java,
            "rivo_database"
        ).allowMainThreadQueries()
            .fallbackToDestructiveMigration()
            .build()
    }
    single { get<RivoDatabase>().privateContactDao() }
    single { get<RivoDatabase>().callNoteDao() }
    single { get<RivoDatabase>().callbackReminderDao() }

    single<IContactsRepository> {
        ContactsRepository(androidContext(), get())
    }
    single<ICallLogRepository> {
        CallLogRepository(androidContext().contentResolver, androidContext(), get())
    }
    single {
        PreferenceManager(androidContext())
    }
    single {
        com.grinch.rivo4.controller.reminder.CallbackReminderManager(androidContext(), get())
    }
    single {
        com.grinch.rivo4.controller.billing.BillingManager(androidContext(), get())
    }
    viewModel { ContactsViewModel(get(), get()) }
    viewModel { CallLogViewModel(get(), androidContext().contentResolver) }
    viewModel { BackupViewModel(get(), get()) }
    viewModel { CallAnalyticsViewModel(get(), get()) }
}