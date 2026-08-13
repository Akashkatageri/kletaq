package com.kletaq.app.features.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kletaq.app.data.repository.KletaqAcademicRepository
import com.kletaq.app.features.journey.components.LessonNode
import com.kletaq.app.features.journey.components.SubjectJourney
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor() : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _matchedSubjects = MutableStateFlow<List<SubjectJourney>>(emptyList())
    val matchedSubjects: StateFlow<List<SubjectJourney>> = _matchedSubjects.asStateFlow()

    private val _matchedTopics = MutableStateFlow<List<Triple<String, String, Pair<String, LessonNode>>>>(emptyList())
    val matchedTopics: StateFlow<List<Triple<String, String, Pair<String, LessonNode>>>> = _matchedTopics.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    init {
        viewModelScope.launch {
            searchQuery
                .debounce(150L)
                .distinctUntilChanged()
                .collectLatest { query ->
                    if (query.isBlank()) {
                        _matchedSubjects.value = emptyList()
                        _matchedTopics.value = emptyList()
                        _isSearching.value = false
                    } else {
                        _isSearching.value = true
                        withContext(Dispatchers.Default) {
                            val semesters = KletaqAcademicRepository.getSemesters()
                            val subjects = semesters.flatMap { sem ->
                                sem.subjects.filter { it.name.contains(query, ignoreCase = true) }
                            }.distinctBy { it.id }

                            val topics = semesters.flatMap { sem ->
                                sem.subjects.flatMap { sub ->
                                    sub.units.flatMap { unit ->
                                        unit.lessons.filter { lesson ->
                                            lesson.title.contains(query, ignoreCase = true) ||
                                            lesson.description.contains(query, ignoreCase = true)
                                        }.map { lesson ->
                                            Triple(sem.id, sub.id, unit.id to lesson)
                                        }
                                    }
                                }
                            }

                            _matchedSubjects.value = subjects
                            _matchedTopics.value = topics
                        }
                        _isSearching.value = false
                    }
                }
        }
    }

    fun onQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }
}
