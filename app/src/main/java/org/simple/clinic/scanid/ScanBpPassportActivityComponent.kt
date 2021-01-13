package org.simple.clinic.scanid

import androidx.appcompat.app.AppCompatActivity
import dagger.BindsInstance
import dagger.Subcomponent
import org.simple.clinic.di.AssistedInjectModule
import org.simple.clinic.widgets.qrcodescanner.QrCodeScannerView

@Subcomponent(modules = [AssistedInjectModule::class])
interface ScanBpPassportActivityComponent :
    ScanSimpleIdScreen.Injector,
    QrCodeScannerView.Injector {

  fun inject(target: ScanBpPassportActivity)

  @Subcomponent.Factory
  interface Factory {
    fun create(@BindsInstance activity: AppCompatActivity): ScanBpPassportActivityComponent
  }
}
