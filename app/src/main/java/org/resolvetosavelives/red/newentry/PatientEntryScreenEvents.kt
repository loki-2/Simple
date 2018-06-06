package org.resolvetosavelives.red.newentry

import org.resolvetosavelives.red.patient.Gender
import org.resolvetosavelives.red.widgets.UiEvent

data class PatientFullNameTextChanged(val fullName: String) : UiEvent

data class PatientPhoneNumberTextChanged(val phoneNumber: String) : UiEvent

data class PatientDateOfBirthTextChanged(val dateOfBirth: String) : UiEvent

data class PatientAgeTextChanged(val age: String) : UiEvent

data class PatientGenderChanged(val gender: Gender) : UiEvent

data class PatientColonyOrVillageTextChanged(val colonyOrVillage: String) : UiEvent

data class PatientDistrictTextChanged(val district: String) : UiEvent

data class PatientStateTextChanged(val state: String) : UiEvent
