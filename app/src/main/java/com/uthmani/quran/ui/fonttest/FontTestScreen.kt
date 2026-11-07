package com.uthmani.quran.ui.fonttest

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.uthmani.quran.R
import com.uthmani.quran.data.QuranRepository
import com.uthmani.quran.ui.theme.QuranFontFamily
import com.uthmani.quran.util.toArabicDigits
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FontTestItem(val surah: Int, val ayah: Int, val text: String?)

/**
 * Debug aid for reviewing font rendering on a real device/emulator.
 * Ayah references only are listed here; every text is read verbatim from the
 * prepackaged database — nothing is hardcoded.
 * Status: NOT VERIFIED by the developer (no Android SDK in the build sandbox) —
 * review this screen on an emulator before release.
 */
@HiltViewModel
class FontTestViewModel @Inject constructor(
    quranRepository: QuranRepository,
) : ViewModel() {

    // Sura 1 full; first ayat of suras starting with disjointed letters (sample);
    // suras 95 and 97 (bismillah form 2 in the data); an aya dense with pause marks;
    // two sajda ayat (from Tanzil metadata).
    private val refs: List<Pair<Int, Int>> = buildList {
        addAll((1..7).map { 1 to it })
        addAll(listOf(2, 7, 10, 19, 20, 29, 36, 38, 40, 42, 50, 68).map { it to 1 })
        add(95 to 1)
        add(97 to 1)
        add(2 to 2)
        add(7 to 206)
        add(53 to 62)
    }

    private val _items = MutableStateFlow<List<FontTestItem>>(emptyList())
    val testItems: StateFlow<List<FontTestItem>> = _items.asStateFlow()

    init {
        viewModelScope.launch {
            _items.value = refs.map { (s, a) ->
                FontTestItem(s, a, quranRepository.ayah(s, a)?.text)
            }
        }
    }
}

@Composable
fun FontTestScreen(
    onBack: () -> Unit,
    viewModel: FontTestViewModel = hiltViewModel(),
) {
    val testItems by viewModel.testItems.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
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
                    text = stringResource(R.string.font_test_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Text(
            text = stringResource(R.string.font_test_note),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        )

        if (testItems.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
        ) {
            items(testItems, key = { "${it.surah}:${it.ayah}" }) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            text = "${item.surah.toArabicDigits()} : ${item.ayah.toArabicDigits()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(6.dp))
                        val text = item.text
                        if (text == null) {
                            Text(
                                text = "—",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        } else {
                            Text(
                                text = text,
                                fontFamily = QuranFontFamily,
                                fontSize = 26.sp,
                                lineHeight = 48.sp,
                                textAlign = TextAlign.Start,
                            )
                        }
                    }
                }
            }
        }
    }
}
