package org.simple.clinic.scheduleappointment

import com.spotify.mobius.Next
import com.spotify.mobius.Update
import org.simple.clinic.mobius.dispatch
import org.simple.clinic.mobius.next
import org.simple.clinic.overdue.PotentialAppointmentDate
import org.simple.clinic.overdue.TimeToAppointment.Days
import org.simple.clinic.util.UserClock
import org.simple.clinic.util.daysTill
import org.threeten.bp.LocalDate

class ScheduleAppointmentUpdate(
    // Added for mobius migration, clock should not be here. Remove this later.
    private val userClock: UserClock
) : Update<ScheduleAppointmentModel, ScheduleAppointmentEvent, ScheduleAppointmentEffect> {

  override fun update(
      model: ScheduleAppointmentModel,
      event: ScheduleAppointmentEvent
  ): Next<ScheduleAppointmentModel, ScheduleAppointmentEffect> {
    return when (event) {
      is DefaultAppointmentDateLoaded -> next(model.appointmentDateSelected(event.potentialAppointmentDate))
      is AppointmentDateIncremented -> selectLaterAppointmentDate(model)
      is AppointmentDateDecremented -> selectEarlierAppointmentDate(model)
      is AppointmentCalendarDateSelected -> selectExactAppointmentDate(event, model)
      ManuallySelectAppointmentDateClicked -> dispatch(ShowDatePicker(model.selectedAppointmentDate!!.scheduledFor))
    }
  }

  private fun selectLaterAppointmentDate(model: ScheduleAppointmentModel): Next<ScheduleAppointmentModel, ScheduleAppointmentEffect> {
    val currentSelectedAppointmentDate = model.selectedAppointmentDate!!
    val laterPotentialAppointmentDate = model.potentialAppointmentDates
        .find { potentialAppointmentDate -> potentialAppointmentDate > currentSelectedAppointmentDate }
        ?: throw RuntimeException("Cannot find configured appointment date after $currentSelectedAppointmentDate")

    return next(model.appointmentDateSelected(laterPotentialAppointmentDate))
  }

  private fun selectEarlierAppointmentDate(model: ScheduleAppointmentModel): Next<ScheduleAppointmentModel, ScheduleAppointmentEffect> {
    val currentSelectedAppointmentDate = model.selectedAppointmentDate!!
    val laterPotentialAppointmentDate = model.potentialAppointmentDates
        .findLast { potentialAppointmentDate -> potentialAppointmentDate < currentSelectedAppointmentDate }
        ?: throw RuntimeException("Cannot find configured appointment date after $currentSelectedAppointmentDate")

    return next(model.appointmentDateSelected(laterPotentialAppointmentDate))
  }

  private fun selectExactAppointmentDate(
      event: AppointmentCalendarDateSelected,
      model: ScheduleAppointmentModel
  ): Next<ScheduleAppointmentModel, ScheduleAppointmentEffect> {
    val userSelectedDate = event.selectedDate

    val today = LocalDate.now(userClock)
    val matchingTimeToAppointmentFromPotentials = model.potentialAppointmentDates
        .find { potentialAppointmentDate -> potentialAppointmentDate.scheduledFor == userSelectedDate }
        ?.timeToAppointment

    val timeToAppointment = matchingTimeToAppointmentFromPotentials ?: Days(today.daysTill(userSelectedDate))
    val potentialAppointmentDate = PotentialAppointmentDate(userSelectedDate, timeToAppointment)

    return next(model.appointmentDateSelected(potentialAppointmentDate))
  }
}
