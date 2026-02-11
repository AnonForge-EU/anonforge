package com.anonforge.domain.model

import kotlinx.datetime.LocalDate

@JvmInline
value class DateOfBirth(val date: LocalDate) {
    val displayFormat: String
        get() = "${date.dayOfMonth}/${date.monthNumber}/${date.year}"
    
    val age: Int
        get() {
            val today = kotlinx.datetime.Clock.System.now()
                .toEpochMilliseconds().let {
                    kotlinx.datetime.Instant.fromEpochMilliseconds(it)
                }
            val todayDate = today.toString().substring(0, 10)
                .let { LocalDate.parse(it) }
            
            var age = todayDate.year - date.year
            if (todayDate.monthNumber < date.monthNumber || 
                (todayDate.monthNumber == date.monthNumber && todayDate.dayOfMonth < date.dayOfMonth)) {
                age--
            }
            return age
        }
}
