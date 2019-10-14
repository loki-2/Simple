package org.simple.clinic.settings

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.Locale

sealed class Language : Parcelable

@Parcelize
data class ProvidedLanguage(val displayName: String, val languageCode: String) : Language() {

  fun matchesLocale(locale: Locale): Boolean {
    val languageTag = locale.toLanguageTag()

    return languageCode.equals(languageTag, ignoreCase = true)
  }
}

@Parcelize
object SystemDefaultLanguage : Language()
