package com.anonforge.domain.model

enum class Gender {
    MALE,
    FEMALE;
    
    companion object {
        fun random(): Gender = entries.random()
    }
}
