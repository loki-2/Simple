package org.simple.clinic.bloodsugar.entry

import com.spotify.mobius.rx2.RxMobius
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import io.reactivex.ObservableTransformer
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.rxkotlin.cast
import org.simple.clinic.ReportAnalyticsEvents
import org.simple.clinic.bloodsugar.BloodSugarMeasurement
import org.simple.clinic.bloodsugar.BloodSugarReading
import org.simple.clinic.bloodsugar.BloodSugarRepository
import org.simple.clinic.bloodsugar.entry.BloodSugarValidator.Result.ErrorBloodSugarEmpty
import org.simple.clinic.bloodsugar.entry.BloodSugarValidator.Result.ErrorBloodSugarTooHigh
import org.simple.clinic.bloodsugar.entry.BloodSugarValidator.Result.ErrorBloodSugarTooLow
import org.simple.clinic.facility.Facility
import org.simple.clinic.facility.FacilityRepository
import org.simple.clinic.overdue.AppointmentRepository
import org.simple.clinic.patient.PatientRepository
import org.simple.clinic.user.User
import org.simple.clinic.user.UserSession
import org.simple.clinic.util.UserClock
import org.simple.clinic.util.exhaustive
import org.simple.clinic.util.filterAndUnwrapJust
import org.simple.clinic.util.scheduler.SchedulersProvider
import org.simple.clinic.util.toLocalDateAtZone
import org.simple.clinic.util.toUtcInstant
import org.simple.clinic.widgets.ageanddateofbirth.UserInputDateValidator
import org.simple.clinic.widgets.ageanddateofbirth.UserInputDateValidator.Result.Invalid.DateIsInFuture
import org.simple.clinic.widgets.ageanddateofbirth.UserInputDateValidator.Result.Invalid.InvalidPattern
import org.simple.clinic.widgets.ageanddateofbirth.UserInputDateValidator.Result.Valid
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate

class BloodSugarEntryEffectHandler @AssistedInject constructor(
    @Assisted private val ui: BloodSugarEntryUi,
    private val userSession: UserSession,
    private val facilityRepository: FacilityRepository,
    private val bloodSugarRepository: BloodSugarRepository,
    private val patientRepository: PatientRepository,
    private val appointmentsRepository: AppointmentRepository,
    private val userClock: UserClock,
    private val schedulersProvider: SchedulersProvider
) {
  @AssistedInject.Factory
  interface Factory {
    fun create(ui: BloodSugarEntryUi): BloodSugarEntryEffectHandler
  }

  private val reportAnalyticsEvents = ReportAnalyticsEvents()

  fun build(): ObservableTransformer<BloodSugarEntryEffect, BloodSugarEntryEvent> {
    return RxMobius
        .subtypeEffectHandler<BloodSugarEntryEffect, BloodSugarEntryEvent>()
        .addAction(HideBloodSugarErrorMessage::class.java, ui::hideBloodSugarErrorMessage, schedulersProvider.ui())
        .addAction(HideDateErrorMessage::class.java, ui::hideDateErrorMessage, schedulersProvider.ui())
        .addAction(Dismiss::class.java, ui::dismiss, schedulersProvider.ui())
        .addAction(ShowDateEntryScreen::class.java, ui::showDateEntryScreen, schedulersProvider.ui())
        .addConsumer(ShowBloodSugarValidationError::class.java, { showBloodSugarValidationError(it.result) }, schedulersProvider.ui())
        .addConsumer(ShowBloodSugarEntryScreen::class.java, { showBloodSugarEntryScreen(it.date) }, schedulersProvider.ui())
        .addTransformer(PrefillDate::class.java, prefillDate(schedulersProvider.ui()))
        .addConsumer(ShowDateValidationError::class.java, { showDateValidationError(it.result) }, schedulersProvider.ui())
        .addAction(SetBloodSugarSavedResultAndFinish::class.java, ui::setBloodSugarSavedResultAndFinish, schedulersProvider.ui())
        .addTransformer(CreateNewBloodSugarEntry::class.java, createNewBloodSugarEntryTransformer())
        .build()
  }

  private fun showBloodSugarValidationError(result: BloodSugarValidator.Result) {
    when (result) {
      ErrorBloodSugarEmpty -> ui.showBloodSugarEmptyError()
      ErrorBloodSugarTooHigh -> ui.showBloodSugarHighError()
      ErrorBloodSugarTooLow -> ui.showBloodSugarLowError()
    }
  }

  private fun showBloodSugarEntryScreen(date: LocalDate) {
    with(ui) {
      showBloodSugarEntryScreen()
      showDateOnDateButton(date)
    }
  }

  private fun prefillDate(scheduler: Scheduler): ObservableTransformer<PrefillDate, BloodSugarEntryEvent> {
    return ObservableTransformer { prefillDates ->
      prefillDates
          .map { Instant.now(userClock).toLocalDateAtZone(userClock.zone) }
          .observeOn(scheduler)
          .doOnNext { setDateOnInputFields(it) }
          .doOnNext { ui.showDateOnDateButton(it) }
          .map { DatePrefilled(it) }
    }
  }

  private fun setDateOnInputFields(dateToSet: LocalDate) {
    ui.setDateOnInputFields(
        dateToSet.dayOfMonth.toString(),
        dateToSet.monthValue.toString(),
        getYear(dateToSet)
    )
  }

  private fun getYear(date: LocalDate): String =
      date.year.toString().substring(startIndex = 2, endIndex = 4)

  private fun showDateValidationError(result: UserInputDateValidator.Result) {
    when (result) {
      InvalidPattern -> ui.showInvalidDateError()
      DateIsInFuture -> ui.showDateIsInFutureError()
      is Valid -> throw IllegalStateException("Date validation error cannot be $result")
    }.exhaustive()
  }

  private fun createNewBloodSugarEntryTransformer(): ObservableTransformer<CreateNewBloodSugarEntry, BloodSugarEntryEvent> {
    return ObservableTransformer { createNewBloodSugarEntries ->
      createNewBloodSugarEntries
          .flatMapSingle { createNewBloodSugarEntry ->
            userAndCurrentFacility()
                .flatMap { (user, facility) -> storeNewBloodSugarMeasurement(user, facility, createNewBloodSugarEntry) }
                .flatMap { updateAppointmentsAsVisited(createNewBloodSugarEntry, it) }
          }
          .compose(reportAnalyticsEvents)
          .cast()
    }
  }

  private fun userAndCurrentFacility(): Single<Pair<User, Facility>> {
    return userSession
        .loggedInUser()
        .filterAndUnwrapJust()
        .flatMap { user ->
          facilityRepository
              .currentFacility(user)
              .map { facility -> user to facility }
        }
        .firstOrError()
  }

  private fun storeNewBloodSugarMeasurement(
      user: User,
      currentFacility: Facility,
      entry: CreateNewBloodSugarEntry
  ): Single<BloodSugarMeasurement> {
    val (patientUuid, bloodSugarReading, measurementType, date) = entry
    return bloodSugarRepository.saveMeasurement(BloodSugarReading(bloodSugarReading, measurementType), patientUuid, user, currentFacility, date.toUtcInstant(userClock))
  }

  private fun updateAppointmentsAsVisited(
      createNewBloodSugarEntry: CreateNewBloodSugarEntry,
      bloodSugarMeasurement: BloodSugarMeasurement
  ): Single<BloodSugarSaved> {
    val entryDate = createNewBloodSugarEntry.userEnteredDate.toUtcInstant(userClock)
    val compareAndUpdateRecordedAt = patientRepository
        .compareAndUpdateRecordedAt(bloodSugarMeasurement.patientUuid, entryDate)

    return appointmentsRepository
        .markAppointmentsCreatedBeforeTodayAsVisited(bloodSugarMeasurement.patientUuid)
        .andThen(compareAndUpdateRecordedAt)
        .toSingleDefault(BloodSugarSaved(createNewBloodSugarEntry.wasDateChanged))
  }
}
