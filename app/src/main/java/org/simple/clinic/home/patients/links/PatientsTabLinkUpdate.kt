package org.simple.clinic.home.patients.links

import com.spotify.mobius.Next
import com.spotify.mobius.Update
import org.simple.clinic.mobius.dispatch

class PatientsTabLinkUpdate :
    Update<PatientsTabLinkModel, PatientsTabLinkEvent, PatientsTabLinkEffect> {
  override fun update(model: PatientsTabLinkModel, event: PatientsTabLinkEvent):
      Next<PatientsTabLinkModel, PatientsTabLinkEffect> {
    return when (event) {
      is CurrentFacilityLoaded -> Next.next(model.currentFacilityLoaded(event.facility))
      is MonthlyScreeningReportResponseListLoaded -> {
        Next.next(model.monthlyScreeningReportResponseListLoaded(event.questionnaireResponseList))
      }
      is MonthlyScreeningReportsClicked -> dispatch(OpenMonthlyScreeningReportsListScreen)
      is DownloadPatientLineListClicked -> dispatch(OpenPatientLineListDownloadDialog)
    }
  }
}
