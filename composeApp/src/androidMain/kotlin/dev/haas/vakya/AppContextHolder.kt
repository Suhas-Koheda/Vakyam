package dev.haas.vakya

import android.content.Context
import androidx.room.Room
import dev.haas.vakya.data.database.VakyaDatabase

object AppContextHolder {
    lateinit var context: Context
        private set

    val database: VakyaDatabase by lazy {
        Room.databaseBuilder(
            context,
            VakyaDatabase::class.java, "vakya-db"
        ).fallbackToDestructiveMigration().build()
    }

    fun initialize(context: Context) {
        this.context = context.applicationContext
    }
}
