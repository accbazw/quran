package com.uthmani.quran.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.uthmani.quran.R
import com.uthmani.quran.data.QuranTextNormalizer
import com.uthmani.quran.data.SearchRepository
import com.uthmani.quran.data.db.SearchRow
import com.uthmani.quran.ui.theme.QuranFontFamily
import com.uthmani.quran.util.toArabicDigits
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
data class SearchUiState(
    val query: String = "",
    val results: List<SearchRow> = emptyList(),
    val loading: Boolean = false,
    val searched: Boolean = false,
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state
                .map { it.query }
                .distinctUntilChanged()
                .collectLatest { raw ->
                    val query = raw.trim()
                    if (query.length < 2) {
                        _state.update {
                            it.copy(results = emptyList(), loading = false, searched = query.isNotEmpty())
                        }
                        return@collectLatest
                    }
                    _state.update { it.copy(loading = true) }
                    delay(250) // debounce
                    val results = searchRepository.search(query)
                    _state.update { it.copy(results = results, loading = false, searched = true) }
                }
        }
    }

    fun onQueryChanged(query: String) {
        _state.update { it.copy(query = query) }
    }
}

@Composable
fun SearchScreen(
    onOpenAyah: (surah: Int, ayah: Int) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                ) {
                    Text(
                        text = stringResource(R.string.tab_search),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                )
                Spacer(Modifier.height(12.dp))
            }
        }

        when {
            state.query.trim().isEmpty() -> Unit // show nothing before typing
            state.query.trim().length < 2 -> {
                EmptyMessage(stringResource(R.string.search_min_query))
            }
            state.loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.results.isEmpty() -> {
                EmptyMessage(stringResource(R.string.search_no_results))
            }
            else -> {
                SelectionContainer {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.results, key = { "${it.surah}:${it.ayah}" }) { row ->
                            SearchResultRow(
                                row = row,
                                query = state.query,
                                onClick = { onOpenAyah(row.surah, row.ayah) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyMessage(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun SearchResultRow(row: SearchRow, query: String, onClick: () -> Unit) {
    val annotated: AnnotatedString = remember(row.text, query) {
        highlight(row.text, query)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                RoundedCornerShape(12.dp),
            )
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = row.namePersian,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = row.ayah.toArabicDigits(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = annotated,
            fontFamily = QuranFontFamily,
            fontSize = 22.sp,
            lineHeight = 40.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun highlight(original: String, rawQuery: String): AnnotatedString =
    buildAnnotatedString {
        append(original)
        val ranges = QuranTextNormalizer.matchRanges(original, rawQuery)
        val highlightStyle = SpanStyle(
            background = androidx.compose.ui.graphics.Color(0xFFFFE08C),
        )
        for (range in ranges) {
            addStyle(highlightStyle, range.first, range.last + 1)
        }
    }
