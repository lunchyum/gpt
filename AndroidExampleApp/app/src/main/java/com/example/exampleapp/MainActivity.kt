package com.lunchyum.neiswallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.expressiveDarkColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.Composable
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
import java.util.Locale

private enum class DisplayMode(val label: String) { TIMETABLE("시간표"), MEAL("급식"), BOTH("둘 다") }
private enum class SchoolLevel(val label: String, val apiName: String) {
    ELEMENTARY("초등학교", "elsTimetable"),
    MIDDLE("중학교", "misTimetable"),
    HIGH("고등학교", "hisTimetable")
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
@Composable
private fun NeisWallpaperApp() {
    val context = LocalContext.current
    val inspection = LocalInspectionMode.current
    val prefs = remember { context.getSharedPreferences("neis_wallpaper", Context.MODE_PRIVATE) }

    var apiKey by remember { mutableStateOf(prefs.getString("apiKey", "") ?: "") }
    var office by remember { mutableStateOf(prefs.getString("office", "B10") ?: "") }
    var schoolCode by remember { mutableStateOf(prefs.getString("schoolCode", "") ?: "") }
    var schoolName by remember { mutableStateOf(prefs.getString("schoolName", "우리 학교") ?: "우리 학교") }
    var grade by remember { mutableStateOf(prefs.getString("grade", "1") ?: "1") }
    var classNo by remember { mutableStateOf(prefs.getString("classNo", "1") ?: "1") }
    var level by remember { mutableStateOf(runCatching { SchoolLevel.valueOf(prefs.getString("level", SchoolLevel.HIGH.name) ?: SchoolLevel.HIGH.name) }.getOrDefault(SchoolLevel.HIGH)) }
    var mode by remember { mutableStateOf(runCatching { DisplayMode.valueOf(prefs.getString("mode", DisplayMode.BOTH.name) ?: DisplayMode.BOTH.name) }.getOrDefault(DisplayMode.BOTH)) }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var loading by remember { mutableStateOf(false) }
    var lessons by remember { mutableStateOf<List<LessonUi>>(emptyList()) }
    var meals by remember { mutableStateOf<List<MealUi>>(emptyList()) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun saveApiKey(value: String) { apiKey = value; prefs.edit().putString("apiKey", value).apply() }
    fun saveOffice(value: String) { office = value; prefs.edit().putString("office", value).apply() }
    fun saveSchoolCode(value: String) { schoolCode = value; prefs.edit().putString("schoolCode", value).apply() }
    fun saveSchoolName(value: String) { schoolName = value; prefs.edit().putString("schoolName", value).apply() }
    fun saveGrade(value: String) { grade = value; prefs.edit().putString("grade", value).apply() }
    fun saveClassNo(value: String) { classNo = value; prefs.edit().putString("classNo", value).apply() }
    fun saveLevel(value: SchoolLevel) { level = value; prefs.edit().putString("level", value.name).apply() }
    fun saveMode(value: DisplayMode) { mode = value; prefs.edit().putString("mode", value.name).apply() }

    fun refresh() {
        if (apiKey.isBlank() || office.isBlank() || schoolCode.isBlank()) {
            scope.launch { snackbar.showSnackbar("API 키, 교육청 코드, 학교 코드를 입력해 주세요.") }
            return
        }
        loading = true
        scope.launch {
            try {
                val client = NeisClient(apiKey)
                val day = date.format(DateTimeFormatter.BASIC_ISO_DATE)
                val nextLessons = if (mode != DisplayMode.MEAL) client.timetable(level.apiName, office, schoolCode, grade, classNo, day)
                    .map { LessonUi(it.period, it.subject) } else emptyList()
                val nextMeals = if (mode != DisplayMode.TIMETABLE) client.meals(office, schoolCode, day)
                    .map { MealUi(it.type, it.items, it.kcal) } else emptyList()
                lessons = nextLessons
                meals = nextMeals
                WallpaperStorage.save(
                    context,
                    WallpaperSnapshot(
                        schoolName = schoolName.ifBlank { "우리 학교" },
                        date = day,
                        mode = mode.name,
                        lessons = nextLessons.map { WallpaperLesson(it.period, it.subject) },
                        meals = nextMeals.map { WallpaperMeal(it.type, it.items, it.kcal) }
                    )
                )
                snackbar.showSnackbar("${date.format(DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN))} 데이터를 저장했습니다.")
            } catch (t: Throwable) {
                snackbar.showSnackbar(t.message ?: "데이터를 불러오지 못했습니다.")
            } finally {
                loading = false
            }
        }
    }

    MaterialExpressiveTheme(
        colorScheme = when {
            inspection -> expressiveLightColorScheme()
            android.os.Build.VERSION.SDK_INT >= 31 && isSystemInDarkTheme() -> dynamicDarkColorScheme(context)
            android.os.Build.VERSION.SDK_INT >= 31 -> dynamicLightColorScheme(context)
            isSystemInDarkTheme() -> expressiveDarkColorScheme()
            else -> expressiveLightColorScheme()
        }
    ) {
        Scaffold(
            topBar = { TopAppBar(title = { Text("오늘의 학교") }) },
            snackbarHost = { SnackbarHost(snackbar) }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(Modifier.height(8.dp)) }
                item { HeroCard(schoolName, date, mode) }
                item {
                    ConfigCard(apiKey, ::saveApiKey, office, ::saveOffice, schoolCode, ::saveSchoolCode, schoolName, ::saveSchoolName)
                }
                item {
                    SchoolCard(level, ::saveLevel, grade, ::saveGrade, classNo, ::saveClassNo, mode, ::saveMode)
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(modifier = Modifier.weight(1f), enabled = !loading, onClick = ::refresh) {
                            Icon(Icons.Default.CloudDownload, null)
                            Spacer(Modifier.width(8.dp))
                            Text("새로고침")
                        }
                        Button(modifier = Modifier.weight(1f), enabled = !loading, onClick = {
                            context.startActivity(Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                                putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(context, NeisLiveWallpaperService::class.java))
                            })
                        }) {
                            Icon(Icons.Default.Wallpaper, null)
                            Spacer(Modifier.width(8.dp))
                            Text("배경화면 적용")
                        }
                    }
                }
                item {
                    AnimatedContent(targetState = loading, label = "loading") { isLoading ->
                        if (isLoading) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            LoadingIndicator()
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                        } else {
                            Text("설정값은 입력하는 즉시 자동 저장됩니다.")
                        }
                    }
                }
                if (lessons.isNotEmpty()) item { TimetablePreview(lessons) }
                if (meals.isNotEmpty()) item { MealPreview(meals) }
                item { Spacer(Modifier.height(28.dp)) }
            }
        }
    }
}

@Composable
private fun HeroCard(school: String, date: LocalDate, mode: DisplayMode) {
    ElevatedCard(modifier = Modifier.fillMaxWidth().animateContentSize(), colors = CardDefaults.elevatedCardColors()) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(date.format(DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN)), style = MaterialTheme.typography.labelLarge)
            Text(school.ifBlank { "우리 학교" }, style = MaterialTheme.typography.headlineMedium)
            Text("${mode.label}를 배경화면에서 한눈에", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun ConfigCard(
    apiKey: String, onApiKey: (String) -> Unit,
    office: String, onOffice: (String) -> Unit,
    schoolCode: String, onSchoolCode: (String) -> Unit,
    schoolName: String, onSchoolName: (String) -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Key, null)
                Spacer(Modifier.width(10.dp))
                Text("설정 · 나이스 연결", style = MaterialTheme.typography.titleLarge)
            }
            Text("API 키는 이 기기에 자동 저장되며, 입력 후 다시 앱을 열어도 유지됩니다.", style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(
                value = apiKey,
                onValueChange = onApiKey,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("나이스 API 키") },
                placeholder = { Text("발급받은 인증키를 입력") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(office, onOffice, Modifier.weight(1f), label = { Text("교육청 코드") }, singleLine = true)
                OutlinedTextField(schoolCode, onSchoolCode, Modifier.weight(1f), label = { Text("학교 코드") }, singleLine = true)
            }
            OutlinedTextField(schoolName, onSchoolName, Modifier.fillMaxWidth(), label = { Text("표시할 학교 이름") }, singleLine = true)
        }
    }
}

@Composable
private fun SchoolCard(
    level: SchoolLevel, onLevel: (SchoolLevel) -> Unit,
    grade: String, onGrade: (String) -> Unit,
    classNo: String, onClassNo: (String) -> Unit,
    mode: DisplayMode, onMode: (DisplayMode) -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("학교·표시 설정", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                SchoolLevel.entries.forEach { value -> FilterChip(selected = level == value, onClick = { onLevel(value) }, label = { Text(value.label) }) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(grade, onGrade, Modifier.weight(1f), label = { Text("학년") }, singleLine = true)
                OutlinedTextField(classNo, onClassNo, Modifier.weight(1f), label = { Text("반") }, singleLine = true)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                DisplayMode.entries.forEach { value ->
                    FilterChip(
                        selected = mode == value,
                        onClick = { onMode(value) },
                        label = { Text(value.label) },
                        leadingIcon = if (mode == value) {
                            { Icon(if (value == DisplayMode.MEAL) Icons.Default.Fastfood else Icons.Default.CalendarMonth, null) }
                        } else null
                    )
                }
            }
        }
    }
}

@Composable
private fun TimetablePreview(lessons: List<LessonUi>) {
    Card(Modifier.fillMaxWidth()) {
        Column {
            ListItem(headlineContent = { Text("시간표", style = MaterialTheme.typography.titleLarge) }, leadingContent = { Icon(Icons.Default.CalendarMonth, null) })
            lessons.forEach { lesson -> ListItem(headlineContent = { Text("${lesson.period}교시") }, supportingContent = { Text(lesson.subject) }) }
        }
    }
}

@Composable
private fun MealPreview(meals: List<MealUi>) {
    Card(Modifier.fillMaxWidth()) {
        Column {
            ListItem(headlineContent = { Text("급식", style = MaterialTheme.typography.titleLarge) }, leadingContent = { Icon(Icons.Default.Fastfood, null) })
            meals.forEach { meal ->
                ListItem(
                    headlineContent = { Text(meal.type) },
                    supportingContent = { Text(meal.items.joinToString(" · ")) },
                    trailingContent = { meal.kcal?.let { Text(it) } }
                )
            }
        }
    }
}
