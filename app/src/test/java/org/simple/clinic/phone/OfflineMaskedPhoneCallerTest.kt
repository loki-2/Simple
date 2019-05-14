package org.simple.clinic.phone

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import io.reactivex.Observable
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.simple.clinic.activity.TheActivity
import org.simple.clinic.util.RxErrorsRule

class OfflineMaskedPhoneCallerTest {

  @get:Rule
  val rxErrorsRule = RxErrorsRule()

  private lateinit var maskedPhoneCaller: MaskedPhoneCaller
  private lateinit var config: PhoneNumberMaskerConfig
  private val caller: Caller = mock()
  private val activity: TheActivity = mock()

  @Before
  fun setUp() {
    maskedPhoneCaller = OfflineMaskedPhoneCaller(Observable.fromCallable { config }, activity)
  }

  @Test
  fun `when masking is disabled then plain phone numbers should be called`() {
    config = PhoneNumberMaskerConfig(maskingEnabled = false, proxyPhoneNumber = "987")

    val plainNumber = "123"

    maskedPhoneCaller.maskedCall(plainNumber, caller = caller).blockingAwait()

    verify(caller).call(activity, "987,123#")
  }

  @Test
  fun `when masking is enabled then masked phone number should be called`() {
    config = PhoneNumberMaskerConfig(maskingEnabled = true, proxyPhoneNumber = "987")
    val plainNumber = "123"

    maskedPhoneCaller.maskedCall(plainNumber, caller = caller).blockingAwait()

    verify(caller).call(activity, "987,123#")
  }
}
