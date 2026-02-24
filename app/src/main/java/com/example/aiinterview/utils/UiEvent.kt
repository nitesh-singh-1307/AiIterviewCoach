package com.example.aiinterview.utils

/**
 * One-shot events emitted from ViewModel → Screen via Channel.
 * These model things that happen ONCE, not persistent UI state:
 *  - Toast/Snackbar messages
 *  - Keyboard dismiss
 *  - Auto-scroll triggers
 */
sealed interface UiEvent {
    data class ShowToast(val message: String) : UiEvent
    data object DismissKeyboard               : UiEvent
    data object ScrollToBottom                : UiEvent
}
