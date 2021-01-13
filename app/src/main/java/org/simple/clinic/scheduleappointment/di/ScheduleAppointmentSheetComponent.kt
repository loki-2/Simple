package org.simple.clinic.scheduleappointment.di

import androidx.appcompat.app.AppCompatActivity
import dagger.BindsInstance
import dagger.Subcomponent
import org.simple.clinic.di.AssistedInjectModule
import org.simple.clinic.scheduleappointment.ScheduleAppointmentSheet

@Subcomponent(modules = [AssistedInjectModule::class])
interface ScheduleAppointmentSheetComponent {

  fun inject(target: ScheduleAppointmentSheet): ScheduleAppointmentSheet

  @Subcomponent.Factory
  interface Factory {
    fun create(@BindsInstance activity: AppCompatActivity): ScheduleAppointmentSheetComponent
  }
}
