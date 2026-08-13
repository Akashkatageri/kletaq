package com.kletaq.app.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kletaq.app.domain.model.HabitTaskItem
import com.kletaq.app.domain.model.MonthlyHabitArchiveDoc
import com.kletaq.app.domain.model.StudyTask
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

object HabitTrackerRepository {

    fun isMonthArchived(yearMonth: YearMonth): Boolean {
        val currentMonth = YearMonth.now()
        return yearMonth.isBefore(currentMonth)
    }

    fun getHabitItemsForMonth(yearMonth: YearMonth): Flow<List<HabitTaskItem>> {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val uid = currentUser?.uid

        val isArchived = isMonthArchived(yearMonth)

        if (isArchived && uid != null) {
            // Lazy load archived month from Firestore snapshot/archive document
            return flow {
                try {
                    val db = FirebaseFirestore.getInstance()
                    val monthKey = String.format("%04d-%02d", yearMonth.year, yearMonth.monthValue)
                    val archiveDocRef = db.collection("users").document(uid).collection("habit_archives").document(monthKey).get().await()

                    if (archiveDocRef.exists()) {
                        val archive = archiveDocRef.toObject(MonthlyHabitArchiveDoc::class.java)
                        if (archive != null) {
                            val items = mutableListOf<HabitTaskItem>()
                            val totalDays = yearMonth.lengthOfMonth()
                            // Active tasks in archive
                            archive.taskCompletions.forEach { (taskId, dates) ->
                                val datesSet = dates.toSet()
                                val (cStreak, lStreak) = calculateStreaks(datesSet, yearMonth)
                                items.add(
                                    HabitTaskItem(
                                        taskId = taskId,
                                        title = "Habit ($taskId)",
                                        categoryEmoji = "📚",
                                        categoryLabel = "Study",
                                        completedDates = datesSet,
                                        isDeleted = false,
                                        completedDaysCount = datesSet.size,
                                        totalDaysInMonth = totalDays,
                                        completionPercentage = (datesSet.size.toFloat() / totalDays * 100).toInt().coerceIn(0, 100),
                                        currentStreak = cStreak,
                                        longestStreak = lStreak
                                    )
                                )
                            }
                            // Deleted tasks in archive
                            archive.deletedTaskSnapshots.forEach { (taskId, snap) ->
                                val datesSet = snap.completedDates.toSet()
                                val (cStreak, lStreak) = calculateStreaks(datesSet, yearMonth)
                                items.add(
                                    HabitTaskItem(
                                        taskId = taskId,
                                        title = snap.title,
                                        categoryEmoji = snap.categoryEmoji,
                                        categoryLabel = snap.categoryLabel,
                                        completedDates = datesSet,
                                        isDeleted = true,
                                        completedDaysCount = datesSet.size,
                                        totalDaysInMonth = totalDays,
                                        completionPercentage = (datesSet.size.toFloat() / totalDays * 100).toInt().coerceIn(0, 100),
                                        currentStreak = cStreak,
                                        longestStreak = lStreak
                                    )
                                )
                            }
                            emit(items)
                            return@flow
                        }
                    }
                } catch (e: Exception) {
                    Log.e("HABIT_REPO", "Error loading archived month: ${yearMonth}", e)
                }
                // Fallback to active tasks filter if archive doc absent
                emit(buildHabitItemsFromTasks(TaskRepository.tasks.value, yearMonth))
            }
        }

        // Current / Active Month: Map active tasks Flow
        return TaskRepository.tasks.map { activeTasks ->
            buildHabitItemsFromTasks(activeTasks, yearMonth)
        }
    }

    private fun buildHabitItemsFromTasks(tasks: List<StudyTask>, yearMonth: YearMonth): List<HabitTaskItem> {
        val totalDays = yearMonth.lengthOfMonth()
        val monthPrefix = String.format("%04d-%02d", yearMonth.year, yearMonth.monthValue)
        return tasks.map { task ->
            val monthCompletedDates = task.completedDates.filter { it.startsWith(monthPrefix) }.toSet()
            val (cStreak, lStreak) = calculateStreaks(task.completedDates.toSet(), yearMonth)
            HabitTaskItem(
                taskId = task.id,
                title = task.title,
                categoryEmoji = task.category.emoji,
                categoryLabel = task.category.label,
                completedDates = monthCompletedDates,
                isDeleted = task.isDeleted,
                completedDaysCount = monthCompletedDates.size,
                totalDaysInMonth = totalDays,
                completionPercentage = (monthCompletedDates.size.toFloat() / totalDays * 100).toInt().coerceIn(0, 100),
                currentStreak = cStreak,
                longestStreak = lStreak
            )
        }
    }

    fun calculateStreaks(completedDates: Set<String>, currentYearMonth: YearMonth): Pair<Int, Int> {
        if (completedDates.isEmpty()) return Pair(0, 0)

        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        val parsedDates = completedDates.mapNotNull {
            try { LocalDate.parse(it, formatter) } catch (e: Exception) { null }
        }.sorted()

        if (parsedDates.isEmpty()) return Pair(0, 0)

        var longestStreak = 0
        var currentRunningStreak = 0
        var prevDate: LocalDate? = null

        for (date in parsedDates) {
            if (prevDate == null) {
                currentRunningStreak = 1
            } else if (date == prevDate.plusDays(1)) {
                currentRunningStreak++
            } else if (date != prevDate) {
                currentRunningStreak = 1
            }
            if (currentRunningStreak > longestStreak) {
                longestStreak = currentRunningStreak
            }
            prevDate = date
        }

        // Active current streak calculation counting backward from today or yesterday
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        var activeStreak = 0

        var checkDate = if (parsedDates.contains(today)) today else if (parsedDates.contains(yesterday)) yesterday else null
        while (checkDate != null && parsedDates.contains(checkDate)) {
            activeStreak++
            checkDate = checkDate.minusDays(1)
        }

        return Pair(activeStreak, longestStreak)
    }

    suspend fun getAvailableMonthKeys(): List<YearMonth> {
        val current = YearMonth.now()
        val currentUser = FirebaseAuth.getInstance().currentUser
        val creationTimeMs = currentUser?.metadata?.creationTimestamp ?: System.currentTimeMillis()

        val creationDate = java.time.Instant.ofEpochMilli(creationTimeMs)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
        var startMonth = YearMonth.of(creationDate.year, creationDate.monthValue)

        if (startMonth.isAfter(current)) {
            startMonth = current
        }

        val months = mutableListOf<YearMonth>()
        var monthIter = startMonth
        while (!monthIter.isAfter(current)) {
            months.add(monthIter)
            monthIter = monthIter.plusMonths(1)
        }
        return months.reversed()
    }
}
