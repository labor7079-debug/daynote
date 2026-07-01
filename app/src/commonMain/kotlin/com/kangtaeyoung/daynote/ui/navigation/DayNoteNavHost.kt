package com.kangtaeyoung.daynote.ui.navigation

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kangtaeyoung.daynote.ui.calendar.CalendarScreen
import com.kangtaeyoung.daynote.ui.components.TopDestination
import com.kangtaeyoung.daynote.ui.ink.InkScreen
import com.kangtaeyoung.daynote.ui.notes.NoteEditorScreen
import com.kangtaeyoung.daynote.ui.notes.NotesListScreen
import com.kangtaeyoung.daynote.ui.search.SearchScreen
import com.kangtaeyoung.daynote.ui.settings.SettingsScreen
import com.kangtaeyoung.daynote.ui.todo.TodoScreen

object Routes {
    const val CALENDAR = "calendar"
    const val NOTES = "notes"
    const val TODO = "todo"
    const val SEARCH = "search"
    const val EDITOR = "editor"
    const val SETTINGS = "settings"
    const val INK = "ink"
}

/** 에디터 진입 스냅샷(forward 복원용). */
private data class EditorArgs(val noteId: String?, val initialDate: Long?)

/**
 * NavController 를 감싸 뒤로/앞으로 이동을 제공한다. Compose Navigation 엔 "앞으로"가 없어
 * [forwardStack] 으로 직접 구현한다. 새 화면 이동 시 forward 기록을 비운다(브라우저 관례).
 * 에디터로 넘기는 noteId·initialDate 는 상태로 들고 forward 스냅샷에 함께 저장한다.
 */
class AppNavigator(val navController: NavHostController) {

    var editorNoteId: String? by mutableStateOf(null)
        private set
    var editorInitialDate: Long? by mutableStateOf(null)
        private set

    private val forwardStack = mutableStateListOf<Pair<String, EditorArgs>>()

    private fun current() = navController.currentBackStackEntry?.destination?.route

    /** 기존 메모 열기. */
    fun openNote(noteId: String) {
        editorNoteId = noteId
        editorInitialDate = null
        forwardStack.clear()
        navController.navigate(Routes.EDITOR)
    }

    /** 새 메모(선택 날짜 자동 주입 가능) 열기. */
    fun openNewNote(initialDate: Long? = null) {
        editorNoteId = null
        editorInitialDate = initialDate
        forwardStack.clear()
        navController.navigate(Routes.EDITOR)
    }

    fun openSearch() {
        forwardStack.clear()
        navController.navigate(Routes.SEARCH)
    }

    fun openSettings() {
        forwardStack.clear()
        navController.navigate(Routes.SETTINGS)
    }

    fun openInk() {
        forwardStack.clear()
        navController.navigate(Routes.INK)
    }

    fun selectTab(dest: TopDestination) {
        forwardStack.clear()
        val route = when (dest) {
            TopDestination.Calendar -> Routes.CALENDAR
            TopDestination.Notes -> Routes.NOTES
            TopDestination.Todo -> Routes.TODO
        }
        navController.navigate(route) {
            popUpTo(Routes.CALENDAR) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun goBack(): Boolean {
        val route = current() ?: return false
        if (navController.previousBackStackEntry == null) return false
        forwardStack.add(route to EditorArgs(editorNoteId, editorInitialDate))
        return navController.popBackStack()
    }

    fun goForward(): Boolean {
        if (forwardStack.isEmpty()) return false
        val (route, args) = forwardStack.removeAt(forwardStack.lastIndex)
        editorNoteId = args.noteId
        editorInitialDate = args.initialDate
        navController.navigate(route)
        return true
    }
}

@Composable
fun DayNoteNavHost() {
    val navController = rememberNavController()
    val navigator = remember(navController) { AppNavigator(navController) }
    val focusRequester = remember { FocusRequester() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .mouseBackForward(
                onBack = { navigator.goBack() },
                onForward = { navigator.goForward() },
            )
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { e ->
                if (e.type == KeyEventType.KeyDown && e.isAltPressed) {
                    when (e.key) {
                        Key.DirectionLeft -> navigator.goBack()
                        Key.DirectionRight -> navigator.goForward()
                        else -> false
                    }
                } else {
                    false
                }
            },
    ) {
        NavHost(navController = navController, startDestination = Routes.CALENDAR) {
            composable(Routes.CALENDAR) {
                CalendarScreen(
                    onOpenNote = { id -> navigator.openNote(id) },
                    onAddNoteForDate = { dateMillis -> navigator.openNewNote(dateMillis) },
                    onSelectDestination = { dest -> navigator.selectTab(dest) },
                    onOpenSettings = { navigator.openSettings() },
                )
            }
            composable(Routes.NOTES) {
                NotesListScreen(
                    onOpenNote = { id -> navigator.openNote(id) },
                    onNewNote = { navigator.openNewNote(null) },
                    onSearch = { navigator.openSearch() },
                    onSelectDestination = { dest -> navigator.selectTab(dest) },
                )
            }
            composable(Routes.TODO) {
                TodoScreen(onSelectDestination = { dest -> navigator.selectTab(dest) })
            }
            composable(Routes.EDITOR) {
                NoteEditorScreen(
                    noteId = navigator.editorNoteId,
                    initialDate = navigator.editorInitialDate,
                    onBack = { navigator.goBack() },
                    onOpenInk = { navigator.openInk() },
                )
            }
            composable(Routes.SEARCH) {
                SearchScreen(
                    onOpenNote = { id -> navigator.openNote(id) },
                    onBack = { navigator.goBack() },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onBack = { navigator.goBack() })
            }
            composable(Routes.INK) {
                InkScreen(onBack = { navigator.goBack() })
            }
        }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}
