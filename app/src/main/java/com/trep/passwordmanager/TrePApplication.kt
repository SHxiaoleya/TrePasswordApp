package com.trep.passwordmanager

import android.app.Application
import com.trep.passwordmanager.data.repository.VaultRepository

class TrePApplication : Application() {

    lateinit var repository: VaultRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = VaultRepository(applicationContext)
    }
}
