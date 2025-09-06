package com.poisonedyouth.nota.notes

import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDateTime

@Service
class NoteHelperService(
    private val clock: Clock,
) {
    fun isOverdue(dueDate: LocalDateTime?): Boolean =
        dueDate?.isBefore(LocalDateTime.now(clock)) ?: false

    fun isUpcoming(dueDate: LocalDateTime?): Boolean {
        if (dueDate == null) return false
        val now = LocalDateTime.now(clock)
        return dueDate.isAfter(now) && dueDate.isBefore(now.plusDays(7))
    }
}
