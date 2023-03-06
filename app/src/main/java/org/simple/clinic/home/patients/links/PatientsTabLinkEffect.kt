package org.simple.clinic.home.patients.links

sealed class PatientsTabLinkEffect

object LoadCurrentFacility : PatientsTabLinkEffect()

object LoadMonthlyScreeningReportResponseList : PatientsTabLinkEffect()

object OpenMonthlyScreeningReportsListScreen : PatientsTabLinkEffect()

object OpenPatientLineListDownloadDialog : PatientsTabLinkEffect()

