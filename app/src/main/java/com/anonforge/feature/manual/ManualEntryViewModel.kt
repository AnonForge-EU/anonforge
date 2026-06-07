package com.anonforge.feature.manual

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anonforge.domain.model.Address
import com.anonforge.domain.model.DateOfBirth
import com.anonforge.domain.model.DomainIdentity
import com.anonforge.domain.model.Email
import com.anonforge.domain.model.ExpiryDuration
import com.anonforge.domain.model.FullName
import com.anonforge.domain.model.Gender
import com.anonforge.domain.model.Nationality
import com.anonforge.domain.model.Phone
import com.anonforge.domain.repository.IdentityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import java.util.UUID
import javax.inject.Inject

/**
 * Form state for manual identity entry. Every field is a plain String/enum so
 * the UI stays a pure function of state. Only first + last name are required;
 * everything else is optional.
 */
data class ManualEntryState(
    val firstName: String = "",
    val lastName: String = "",
    val middleName: String = "",
    val gender: Gender = Gender.MALE,
    val nationality: Nationality = Nationality.DEFAULT,
    val dobDay: String = "",
    val dobMonth: String = "",
    val dobYear: String = "",
    val phone: String = "",
    val street: String = "",
    val city: String = "",
    val zipCode: String = "",
    val country: String = "",
    val email: String = "",
    val customName: String = "",
    val expiry: ExpiryDuration = ExpiryDuration.PERMANENT,
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val error: String? = null
) {
    /** Phone must be empty or valid E.164. */
    val phoneError: Boolean
        get() = phone.isNotBlank() && !phone.trim().matches(Regex("^\\+[1-9]\\d{1,14}$"))

    /** True if the user entered DOB fields that don't form a real calendar date. */
    val dobError: Boolean
        get() {
            if (dobDay.isBlank() && dobMonth.isBlank() && dobYear.isBlank()) return false
            return parseDob() == null
        }

    /** Parses the DOB fields into a LocalDate, or null if incomplete/invalid. */
    fun parseDob(): LocalDate? {
        val d = dobDay.toIntOrNull() ?: return null
        val m = dobMonth.toIntOrNull() ?: return null
        val y = dobYear.toIntOrNull() ?: return null
        if (y < 1900 || y > 2100) return null
        return try {
            LocalDate(y, m, d)
        } catch (_: Exception) {
            null
        }
    }

    val canSave: Boolean
        get() = firstName.isNotBlank() && lastName.isNotBlank() && !phoneError && !dobError
}

@HiltViewModel
class ManualEntryViewModel @Inject constructor(
    private val identityRepository: IdentityRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ManualEntryState())
    val state: StateFlow<ManualEntryState> = _state.asStateFlow()

    fun update(transform: (ManualEntryState) -> ManualEntryState) {
        _state.update(transform)
    }

    fun save() {
        val s = _state.value
        if (!s.canSave) {
            _state.update { it.copy(error = "First and last name are required") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            try {
                val now = Clock.System.now()
                // DOB is non-nullable in the model; default to 2000-01-01 when
                // the user left it blank (they can edit it later).
                val dob = s.parseDob() ?: LocalDate(2000, 1, 1)

                val address = if (
                    s.street.isBlank() && s.city.isBlank() &&
                    s.zipCode.isBlank() && s.country.isBlank()
                ) {
                    null
                } else {
                    Address(
                        street = s.street.trim(),
                        city = s.city.trim(),
                        zipCode = s.zipCode.trim(),
                        country = s.country.trim()
                    )
                }

                val identity = DomainIdentity(
                    id = UUID.randomUUID().toString(),
                    fullName = FullName(
                        firstName = s.firstName.trim(),
                        middleName = s.middleName.trim().ifBlank { null },
                        lastName = s.lastName.trim(),
                        gender = s.gender
                    ),
                    email = s.email.trim().ifBlank { null }?.let { Email(it) },
                    phone = Phone(s.phone.trim()), // empty allowed = no phone
                    address = address,
                    dateOfBirth = DateOfBirth(dob),
                    createdAt = now,
                    expiresAt = if (s.expiry == ExpiryDuration.PERMANENT) {
                        null
                    } else {
                        now + s.expiry.duration
                    },
                    gender = s.gender,
                    nationality = s.nationality,
                    customName = s.customName.trim().ifBlank { null }
                )

                identityRepository.insertIdentity(identity)
                _state.update { it.copy(isSaving = false, savedSuccessfully = true) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isSaving = false, error = "Failed to save: ${e.message}")
                }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
