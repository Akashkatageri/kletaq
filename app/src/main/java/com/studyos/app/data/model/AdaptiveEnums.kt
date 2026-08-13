package com.studyos.app.data.model

enum class MemoryHealth(val value: String) {
    FRESH("fresh"),
    STABLE("stable"),
    FADING("fading"),
    WEAK("weak"),
    CRITICAL("critical");

    companion object {
        fun fromString(str: String): MemoryHealth {
            return entries.firstOrNull { it.value.equals(str, ignoreCase = true) || it.name.equals(str, ignoreCase = true) }
                ?: STABLE
        }
    }
}
