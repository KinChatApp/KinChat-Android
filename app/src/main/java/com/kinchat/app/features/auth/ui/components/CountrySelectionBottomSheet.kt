package com.kinchat.app.features.auth.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kinchat.app.core.utils.COUNTRIES

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountrySelectionBottomSheet(
    onDismissRequest: () -> Unit,
    onCountrySelected: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        windowInsets = WindowInsets.navigationBars
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            items(COUNTRIES) { country ->
                ListItem(
                    headlineContent = {
                        Text(
                            text = "${country.flag}  ${country.name} (${country.code})",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    modifier = Modifier.clickable {
                        onCountrySelected(country.code)
                    }
                )
            }
        }
    }
}
