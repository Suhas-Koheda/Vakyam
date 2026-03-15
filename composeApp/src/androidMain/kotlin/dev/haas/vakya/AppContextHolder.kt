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

    val gemmaParser: dev.haas.vakya.ai.GemmaParser by lazy {
        dev.haas.vakya.ai.GemmaParser()
    }

    private val retrofit: retrofit2.Retrofit by lazy {
        retrofit2.Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/")
            .addConverterFactory(retrofit2.converter.moshi.MoshiConverterFactory.create())
            .build()
    }

    val gmailApi: dev.haas.vakya.data.google.GmailApi by lazy {
        retrofit.create(dev.haas.vakya.data.google.GmailApi::class.java)
    }

    val calendarApi: dev.haas.vakya.data.google.CalendarApi by lazy {
        retrofit.create(dev.haas.vakya.data.google.CalendarApi::class.java)
    }

    fun initialize(context: Context) {
        this.context = context.applicationContext
    }

    fun close() {
        gemmaParser.close()
    }
}
