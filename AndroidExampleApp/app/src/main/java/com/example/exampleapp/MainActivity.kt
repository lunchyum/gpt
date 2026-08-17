package com.lunchyum.neiswallpaper

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.app.WallpaperManager
import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.expressiveDarkColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private enum class DisplayMode(val label: String) {
    TIMETABLE("시간표"), MEAL("급식"), BOTH("둘 다")
}

private enum class SchoolLevel(val label: String, val apiName: String) {
    ELEMENTARY("초등학교", "elsTimetable"), MIDDLE("중학교", "misTimetable"), HIGH("고등학교", "hisTimetable")
}

private data class LessonUi(val period: Int, val subject: String)
private data class MealUi(val type: String, val items: List<String>, val kcal: String?)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NeisWallpaperApp() }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@androidx.compose.runtime.Composable
private fun NeisWallpaperApp() {
    val context = LocalContext.current
    val inspection = LocalInspectionMode.current
    val prefs = remember { context.getSharedPreferences("neis_wallpaper", Context.MODE_PRIVATE) }
    var apiKey by remember { mutableStateOf(prefs.getString("apiKey", "") ?: "") }
    var office by remember { mutableStateOf(prefs.getString("office", "B10") ?: "B10") }
    var schoolCode by remember { mutableStateOf(prefs.getString("schoolCode", "") ?: "") }
    var schoolName by remember { mutableStateOf(prefs.getString("schoolName", "우리 학교") ?: "우리 학교") }
    var grade by remember { mutableStateOf(prefs.getString("grade", "1") ?: "1") }
    var classNo by remember { mutableStateOf(prefs.getString("classNo", "1") ?: "1") }
    var level by remember { mutableStateOf(SchoolLevel.valueOf(prefs.getString("level", SchoolLevel.HIGH.name) ?: SchoolLevel.HIGH.name)) }
    var mode by remember { mutableStateOf(DisplayMode.valueOf(prefs.getString("mode", DisplayMode.BOTH.name) ?: DisplayMode.BOTH.name)) }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var loading by remember { mutableStateOf(false) }
    var lessons by remember { mutableStateOf<List<LessonUi>>(emptyList()) }
    var meals by remember { mutableStateOf<List<MealUi>>(emptyList()) }
    var message by remember { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun saveConfig() {
        prefs.edit().apply {
            putString("apiKey", apiKey); putString("office", office); putString("schoolCode", schoolCode)
            putString("schoolName", schoolName); putString("grade", grade); putString("classNo", classNo)
            putString("level", level.name); putString("mode", mode.name); apply()
        }
    }

    fun refresh() {
        if (apiKey.isBlank() || office.isBlank() || schoolCode.isBlank()) {
            message = "API 키, 교육청 코드, 학교 코드를 입력해 주세요."
            return
        }
        loading = true
        message = null
        saveConfig()
        scope.launch {
            try {
                val client = NeisClient(apiKey)
                val day = date.format(DateTimeFormatter.BASIC_ISO_DATE)
                val nextLessons = if (mode != DisplayMode.MEAL) {
                    client.timetable(level.apiName, office, schoolCode, grade, classNo, day).map { LessonUi(it.period, it.subject) }
                } else emptyList()
                val nextMeals = if (mode != DisplayMode.TIMETABLE) {
                    client.meals(office, schoolCode, day).map { MealUi(it.type, it.items, it.kcal) }
                } else emptyList()
                lessons = nextLessons
                meals = nextMeals
                val snapshot = WallpaperSnapshot(
                    schoolName = schoolName.ifBlank { "우리 학교" }, date = day, mode = mode.name,
                    lessons = nextLessons.map { WallpaperLesson(it.period, it.subject) },
                    meals = nextMeals.map { WallpaperMeal(it.type, it.items, it.kcal) }
                )
                WallpaperStorage.save(context, snapshot)
                message = "${date.format(DateTimeFormatter.ofPattern("M월 d일"))} 데이터를 업데이트했습니다."
                snackbar.showSnackbar(message!!)
            } catch (t: Throwable) {
                message = t.message ?: "데이터를 불러오지 못했습니다."
                snackbar.showSnackbar(message!!)
            } finally { loading = false }
        }
    }

    MaterialExpressiveTheme(
        colorScheme = when {
            inspection -> expressiveLightColorScheme()
            android.os.Build.VERSION.SDK_INT >= 31 && androidx.compose.foundation.isSystemInDarkTheme() -> dynamicDarkColorScheme(context)
            android.os.Build.VERSION.SDK_INT >= 31 -> dynamicLightColorScheme(context)
            androidx.compose.foundation.isSystemInDarkTheme() -> expressiveDarkColorScheme()
            else -> expressiveLightColorScheme()
        }
    ) {
        Scaffold(
            topBar = { TopAppBar(title = { Text("오늘의 학교") }, actions = {
                IconButton(onClick = { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }) { Icon(Icons.Default.Settings, "설정") }
            }, colors = TopAppBarDefaults.topAppBarColors()) },
            snackbarHost = { SnackbarHost(snackbar) }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(Modifier.height(8.dp)) }
                item { HeroCard(schoolName, date, mode, loading) }
                item {
                    ConfigCard(apiKey, { apiKey = it }, office, { office = it }, schoolCode, { schoolCode = it }, schoolName, { schoolName = it })
                }
                item {
                    SchoolCard(level, { level = it }, grade, { grade = it }, classNo, { classNo = it }, mode, { mode = it })
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(modifier = Modifier.weight(1f), enabled = !loading, onClick = { refresh() }) {
                            Icon(Icons.Default.CloudDownload, null); Spacer(Modifier.width(8.dp)); Text("새로고침")
                        }
                        Button(modifier = Modifier.weight(1f), enabled = !loading, onClick = {
                            saveConfig()
                            context.startActivity(Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                                putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(context, NeisLiveWallpaperService::class.java))
                            })
                        }) {
                            Icon(Icons.Default.Wallpaper, null); Spacer(Modifier.width(8.dp)); Text("배경화면 적용")
                        }
                    }
                }
                item { AnimatedContent(targetState = loading, label = "loading") { isLoading ->
                    if (isLoading) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { LoadingIndicator(); LinearProgressIndicator(Modifier.fillMaxWidth()) }
                    else Text(message ?: "원하는 데이터를 선택한 뒤 배경화면에 적용하세요.")
                } }
                if (lessons.isNotEmpty()) item { TimetablePreview(lessons) }
                if (meals.isNotEmpty()) item { MealPreview(meals) }
                item { Spacer(Modifier.height(28.dp)) }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun HeroCard(school: String, date: LocalDate, mode: DisplayMode, loading: Boolean) {
    ElevatedCard(modifier = Modifier.fillMaxWidth().animateContentSize(), colors = CardDefaults.elevatedCardColors()) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${date.format(DateTimeFormatter.ofPattern("M월 d일 (E)", java.util.Locale.KOREAN))}", style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
            Text(school.ifBlank { "우리 학교" }, style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
            Text("${mode.label}를 잠금화면처럼 한눈에", style = androidx.compose.material3.MaterialTheme.typography.bodyLarge)
        }
    }
}

@androidx.compose.runtime.Composable
private fun ConfigCard(apiKey: String, onApiKey: (String) -> Unit, office: String, onOffice: (String) -> Unit,
                       schoolCode: String, onSchoolCode: (String) -> Unit, schoolName: String, onSchoolName: (String) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Key, null); Spacer(Modifier.width(10.dp)); Text("나이스 연결", style = androidx.compose.material3.MaterialTheme.typography.titleLarge) }
            OutlinedTextField(apiKey, onApiKey, Modifier.fillMaxWidth(), label = { Text("나이스 API 키") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(office, onOffice, Modifier.weight(1f), label = { Text("교육청 코드") }, singleLine = true)
                OutlinedTextField(schoolCode, onSchoolCode, Modifier.weight(1f), label = { Text("학교 코드") }, singleLine = true)
            }
            OutlinedTextField(schoolName, onSchoolName, Modifier.fillMaxWidth(), label = { Text("표시할 학교 이름") }, singleLine = true)
        }
    }
}

@androidx.compose.runtime.Composable
private fun SchoolCard(level: SchoolLevel, onLevel: (SchoolLevel) -> Unit, grade: String, onGrade: (String) -> Unit,
                       classNo: String, onClassNo: (String) -> Unit, mode: DisplayMode, onMode: (DisplayMode) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("표시할 내용", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                SchoolLevel.entries.forEach { value -> FilterChip(selected = level == value, onClick = { onLevel(value) }, label = { Text(value.label) }, leadingIcon = { if (level == value) Icon(Icons.Default.CalendarMonth, null) }) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(grade, onGrade, Modifier.weight(1f), label = { Text("학년") }, singleLine = true)
                OutlinedTextField(classNo, onClassNo, Modifier.weight(1f), label = { Text("반") }, singleLine = true)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                DisplayMode.entries.forEach { value -> FilterChip(selected = mode == value, onClick = { onMode(value) }, label = { Text(value.label) }, leadingIcon = {
                    if (mode == value) Icon(if (value == DisplayMode.MEAL) Icons.Default.Fastfood else Icons.Default.CalendarMonth, null)
                }) }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun TimetablePreview(lessons: List<LessonUi>) {
    Card(Modifier.fillMaxWidth()) {
        Column {
            ListItem(headlineContent = { Text("시간표", style = androidx.compose.material3.MaterialTheme.typography.titleLarge) }, leadingContent = { Icon(Icons.Default.CalendarMonth, null) })
            lessons.forEach { lesson -> ListItem(headlineContent = { Text("${lesson.period}교시") }, supportingContent = { Text(lesson.subject) }) }
        }
    }
}

@androidx.compose.runtime.Composable
private fun MealPreview(meals: List<MealUi>) {
    Card(Modifier.fillMaxWidth()) {
        Column {
            ListItem(headlineContent = { Text("급식", style = androidx.compose.material3.MaterialTheme.typography.titleLarge) }, leadingContent = { Icon(Icons.Default.Fastfood, null) })
            meals.forEach { meal -> ListItem(headlineContent = { Text(meal.type) }, supportingContent = { Text(meal.items.joinToString(" · ")) }, trailingContent = { meal.kcal?.let { Text(it) } }) }
        }
    }
}
