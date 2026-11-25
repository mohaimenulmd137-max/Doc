package com.example.export

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ExportPhase {
    IDLE,
    PREPARING,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class ExportProgressState(
    val phase: ExportPhase = ExportPhase.IDLE,
    val projectId: String = "",
    val projectTitle: String = "",
    val currentChunkIndex: Int = 0,
    val totalChunks: Int = 1,
    val currentChunkTitle: String = "",
    val overallPercent: Int = 0,
    val outputFiles: List<String> = emptyList(),
    val chaptersTxtPath: String? = null,
    val errorMessage: String? = null,
    val elapsedMs: Long = 0L
)

object ExportStateHolder {
    private val _state = MutableStateFlow(ExportProgressState())
    val state: StateFlow<ExportProgressState> = _state.asStateFlow()

    fun update(transform: (ExportProgressState) -> ExportProgressState) {
        _state.value = transform(_state.value)
    }

    fun reset() {
        _state.value = ExportProgressState()
    }
}
