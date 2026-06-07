package com.anonforge.feature.manual

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anonforge.R
import com.anonforge.domain.model.ExpiryDuration
import com.anonforge.domain.model.Gender
import com.anonforge.domain.model.Nationality
import com.anonforge.ui.components.SecureScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualEntryScreen(
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: ManualEntryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) onSaved()
    }
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    SecureScreen {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.manual_title)) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── Identity ───────────────────────────────────────────────
                SectionLabel(stringResource(R.string.manual_section_identity))
                OutlinedTextField(
                    value = state.firstName,
                    onValueChange = { v -> viewModel.update { it.copy(firstName = v) } },
                    label = { Text(stringResource(R.string.manual_first_name)) },
                    isError = state.firstName.isBlank() && state.lastName.isNotBlank(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.lastName,
                    onValueChange = { v -> viewModel.update { it.copy(lastName = v) } },
                    label = { Text(stringResource(R.string.manual_last_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.middleName,
                    onValueChange = { v -> viewModel.update { it.copy(middleName = v) } },
                    label = { Text(stringResource(R.string.manual_middle_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Gender
                Text(
                    stringResource(R.string.manual_gender),
                    style = MaterialTheme.typography.labelLarge
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Gender.entries.forEach { g ->
                        FilterChip(
                            selected = state.gender == g,
                            onClick = { viewModel.update { it.copy(gender = g) } },
                            label = { Text(genderLabel(g)) }
                        )
                    }
                }

                // Nationality
                Text(
                    stringResource(R.string.manual_nationality),
                    style = MaterialTheme.typography.labelLarge
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Nationality.entries.forEach { n ->
                        FilterChip(
                            selected = state.nationality == n,
                            onClick = { viewModel.update { it.copy(nationality = n) } },
                            label = { Text(nationalityLabel(n)) }
                        )
                    }
                }

                // ── Date of birth ──────────────────────────────────────────
                SectionLabel(stringResource(R.string.manual_dob))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.dobDay,
                        onValueChange = { v -> viewModel.update { it.copy(dobDay = v.filter { c -> c.isDigit() }.take(2)) } },
                        label = { Text(stringResource(R.string.manual_dob_day)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = state.dobError,
                        singleLine = true,
                        modifier = Modifier.width(90.dp)
                    )
                    OutlinedTextField(
                        value = state.dobMonth,
                        onValueChange = { v -> viewModel.update { it.copy(dobMonth = v.filter { c -> c.isDigit() }.take(2)) } },
                        label = { Text(stringResource(R.string.manual_dob_month)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = state.dobError,
                        singleLine = true,
                        modifier = Modifier.width(90.dp)
                    )
                    OutlinedTextField(
                        value = state.dobYear,
                        onValueChange = { v -> viewModel.update { it.copy(dobYear = v.filter { c -> c.isDigit() }.take(4)) } },
                        label = { Text(stringResource(R.string.manual_dob_year)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = state.dobError,
                        singleLine = true,
                        modifier = Modifier.width(110.dp)
                    )
                }
                if (state.dobError) {
                    Text(
                        stringResource(R.string.manual_dob_invalid),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                // ── Contact ────────────────────────────────────────────────
                SectionLabel(stringResource(R.string.manual_section_contact))
                OutlinedTextField(
                    value = state.phone,
                    onValueChange = { v -> viewModel.update { it.copy(phone = v) } },
                    label = { Text(stringResource(R.string.manual_phone)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    isError = state.phoneError,
                    supportingText = if (state.phoneError) {
                        { Text(stringResource(R.string.manual_phone_invalid)) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.email,
                    onValueChange = { v -> viewModel.update { it.copy(email = v) } },
                    label = { Text(stringResource(R.string.manual_email)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // ── Address ────────────────────────────────────────────────
                SectionLabel(stringResource(R.string.manual_address))
                OutlinedTextField(
                    value = state.street,
                    onValueChange = { v -> viewModel.update { it.copy(street = v) } },
                    label = { Text(stringResource(R.string.manual_street)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.zipCode,
                        onValueChange = { v -> viewModel.update { it.copy(zipCode = v) } },
                        label = { Text(stringResource(R.string.manual_zip)) },
                        singleLine = true,
                        modifier = Modifier.width(140.dp)
                    )
                    OutlinedTextField(
                        value = state.city,
                        onValueChange = { v -> viewModel.update { it.copy(city = v) } },
                        label = { Text(stringResource(R.string.manual_city)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = state.country,
                    onValueChange = { v -> viewModel.update { it.copy(country = v) } },
                    label = { Text(stringResource(R.string.manual_country)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // ── Label & expiry ─────────────────────────────────────────
                SectionLabel(stringResource(R.string.manual_section_other))
                OutlinedTextField(
                    value = state.customName,
                    onValueChange = { v -> viewModel.update { it.copy(customName = v) } },
                    label = { Text(stringResource(R.string.manual_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    stringResource(R.string.manual_expiry),
                    style = MaterialTheme.typography.labelLarge
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ExpiryDuration.entries.forEach { e ->
                        FilterChip(
                            selected = state.expiry == e,
                            onClick = { viewModel.update { it.copy(expiry = e) } },
                            label = { Text(expiryLabel(e)) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.save() },
                    enabled = state.canSave && !state.isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.manual_save))
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun genderLabel(g: Gender): String = when (g) {
    Gender.MALE -> stringResource(R.string.gender_male)
    Gender.FEMALE -> stringResource(R.string.gender_female)
}

@Composable
private fun nationalityLabel(n: Nationality): String = when (n) {
    Nationality.FR -> stringResource(R.string.nationality_fr)
    Nationality.EN -> stringResource(R.string.nationality_en)
    Nationality.DE -> stringResource(R.string.nationality_de)
}

@Composable
private fun expiryLabel(e: ExpiryDuration): String = when (e) {
    ExpiryDuration.ONE_DAY -> stringResource(R.string.expiry_1d)
    ExpiryDuration.ONE_WEEK -> stringResource(R.string.expiry_1w)
    ExpiryDuration.ONE_MONTH -> stringResource(R.string.expiry_1m)
    ExpiryDuration.PERMANENT -> stringResource(R.string.expiry_permanent)
}
