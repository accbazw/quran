package com.uthmani.quran.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.uthmani.quran.R
import com.uthmani.quran.data.AppSettings
import com.uthmani.quran.data.BookmarkRepository
import com.uthmani.quran.data.QuranRepository
import com.uthmani.quran.data.QuranText
import com.uthmani.quran.data.SettingsRepository
import com.uthmani.quran.data.db.AyahEntity
import com.uthmani.quran.data.db.SurahEntity
import com.uthmani.quran.ui.theme.QuranFontFamily
import com.uthmani.quran.util.toArabicDigits
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.remember

private const val ORNATE_OPEN = '\uFD3F' // ﴿
private const val ORNATE_CLOSE = '\uFD3E' // ﴾
private const val END_OF_AYAH = '\u06DD'

@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val quranRepository: QuranRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val surahNumber: Int = savedStateHandle.get<Int>("surah") ?: 1
    val initialAyah: Int = savedStateHandle.get<Int>("ayah") ?: 0

    val surah: StateFlow<SurahEntity?> = quranRepository.observeSurah(surahNumber)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val ayahs: StateFlow<List<AyahEntity>> = quranRepository.observeAyahs(surahNumber)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val bookmarked: StateFlow<Set<Int>> = bookmarkRepository.observeBookmarkedAyahs(surahNumber)
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val fontSize: StateFlow<Int> = settingsRepository.settings
        .map { it.fontSize }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings.FONT_SIZE_DEFAULT)

    private val _selectedAyah = MutableStateFlow<Int?>(null)
    val selectedAyah: StateFlow<Int?> = _selectedAyah.asStateFlow()

    fun select(ayah: Int?) {
        _selectedAyah.value = if (_selectedAyah.value == ayah) null else ayah
    }

    fun toggleBookmark(ayah: Int) {
        viewModelScope.launch { bookmarkRepository.toggle(surahNumber, ayah) }
    }

    fun saveProgress(ayah: Int) {
        viewModelScope.launch { settingsRepository.setLastRead(surahNumber, ayah) }
    }
}

@Composable
fun ReaderScreen(
    onBack: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel(),
) {
    val surah by viewModel.surah.collectAsStateWithLifecycle()
    val ayahs by viewModel.ayahs.collectAsStateWithLifecycle()
    val bookmarked by viewModel.bookmarked.collectAsStateWithLifecycle()
    val selectedAyah by viewModel.selectedAyah.collectAsStateWithLifecycle()
    val fontSize by viewModel.fontSize.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()

    // bismillah header (display layer only; stored text untouched)
    val bismillahHeader = surah?.let { QuranText.bismillahHeader(it.number) }
    // list layout: [0]=surah header, [1]=bismillah (optional), then ayahs
    val headerCount = 1 + (if (bismillahHeader != null) 1 else 0)

    // initial scroll (only once, when data first arrives)
    var didInitialScroll by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(ayahs.isNotEmpty(), bismillahHeader) {
        if (!didInitialScroll && ayahs.isNotEmpty() && viewModel.initialAyah > 0) {
            didInitialScroll = true
            listState.scrollToItem(headerCount + viewModel.initialAyah - 1)
        }
    }

    // save last-read position (debounced)
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collectLatest { index ->
                delay(800)
                val size = viewModel.ayahs.value.size
                val surahNumber = viewModel.surah.value?.number ?: return@collectLatest
                val hasBismillah = QuranText.bismillahHeader(surahNumber) != null
                val ayah = index - 1 - (if (hasBismillah) 1 else 0) + 1
                if (ayah in 1..size) {
                    viewModel.saveProgress(ayah)
                }
            }
    }

    Scaffold(
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                    Text(
                        text = surah?.namePersian ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
        bottomBar = {
            selectedAyah?.let { ayah ->
                Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val isBookmarked = bookmarked.contains(ayah)
                        IconButton(onClick = { viewModel.toggleBookmark(ayah) }) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Filled.Star else Icons.Filled.StarBorder,
                                contentDescription = stringResource(
                                    if (isBookmarked) R.string.bookmark_remove else R.string.bookmark_add,
                                ),
                                tint = if (isBookmarked) {
                                    MaterialTheme.colorScheme.tertiary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                        Text(
                            text = stringResource(
                                if (isBookmarked) R.string.bookmark_added else R.string.bookmark_add,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { viewModel.select(null) }) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = stringResource(R.string.selected_ayah_close),
                            )
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        if (ayahs.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
            // Surah header card
            item(key = "header") {
                surah?.let { s ->
                    SurahHeaderCard(s)
                }
            }
            // Bismillah (display layer only)
            bismillahHeader?.let { header ->
                item(key = "bismillah") {
                    Text(
                        text = header,
                        fontFamily = QuranFontFamily,
                        fontSize = (fontSize + 2).sp,
                        lineHeight = ((fontSize + 2) * 1.9f).sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            // Ayahs
            itemsIndexed(ayahs, key = { _, a -> a.ayah }) { _, ayahEntity ->
                AyahRow(
                    ayah = ayahEntity,
                    displayText = QuranText.displayAyahText(
                        ayahEntity.surah, ayahEntity.ayah, ayahEntity.text,
                    ),
                    fontSize = fontSize,
                    isSelected = selectedAyah == ayahEntity.ayah,
                    isBookmarked = bookmarked.contains(ayahEntity.ayah),
                    onClick = { viewModel.select(ayahEntity.ayah) },
                )
            }
            }
        }
    }
}

@Composable
private fun SurahHeaderCard(surah: SurahEntity) {
    val revelationLabel = if (surah.revelationType == "Meccan") {
        stringResource(R.string.revelation_meccan)
    } else {
        stringResource(R.string.revelation_medinan)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                RoundedCornerShape(16.dp),
            )
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = surah.nameArabic,
            fontFamily = QuranFontFamily,
            fontSize = 28.sp,
            lineHeight = 50.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${surah.namePersian} · " +
                stringResource(R.string.ayah_count_format, surah.ayahCount) +
                " · $revelationLabel",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun AyahRow(
    ayah: AyahEntity,
    displayText: String,
    fontSize: Int,
    isSelected: Boolean,
    isBookmarked: Boolean,
    onClick: () -> Unit,
) {
    val annotated: AnnotatedString = remember(displayText, ayah.ayah) {
        buildAnnotatedString {
            append(displayText)
            // Add an ornate aya number for display unless the text already carries one.
            val hasMarker = displayText.any { it == ORNATE_OPEN || it == ORNATE_CLOSE || it == END_OF_AYAH }
            if (!hasMarker) {
                append(' ')
                append(ORNATE_OPEN)
                append(ayah.ayah.toArabicDigits())
                append(ORNATE_CLOSE)
            }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
                RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        if (isBookmarked) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(4.dp))
            }
        }
        Text(
            text = annotated,
            fontFamily = QuranFontFamily,
            fontSize = fontSize.sp,
            lineHeight = (fontSize * 1.9f).sp,
            textAlign = TextAlign.Start,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
