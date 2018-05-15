package org.resolvetosavelives.red.newentry.search

import android.content.Context
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.widget.Button
import android.widget.EditText
import android.widget.RelativeLayout
import com.jakewharton.rxbinding2.view.RxView
import com.jakewharton.rxbinding2.widget.RxTextView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.Schedulers.io
import kotterknife.bindView
import org.resolvetosavelives.red.R
import org.resolvetosavelives.red.TheActivity
import org.resolvetosavelives.red.newentry.personal.PatientPersonalDetailsEntryScreen
import org.resolvetosavelives.red.router.screen.ScreenRouter
import org.resolvetosavelives.red.widgets.showKeyboard
import javax.inject.Inject

class PatientSearchByMobileScreen(context: Context, attrs: AttributeSet) : RelativeLayout(context, attrs) {

  companion object {
    val KEY = PatientSearchByMobileScreenKey()
  }

  private val mobileNumberEditText by bindView<EditText>(R.id.patientsearch_mobile_number)
  private val newPatientButton by bindView<Button>(R.id.patientsearch_new_patient)
  private val patientRecyclerView by bindView<RecyclerView>(R.id.patientsearch_recyclerview)

  @Inject
  lateinit var screenRouter: ScreenRouter

  @Inject
  lateinit var patientRepository: PatientRepository

  override fun onFinishInflate() {
    super.onFinishInflate()
    if (isInEditMode) {
      return
    }
    TheActivity.component.inject(this)

    mobileNumberEditText.showKeyboard()
    setupPatientSearchResults()

    newPatientButton.setOnClickListener({
      val ongoingEntry = OngoingPatientEntry(null, mobileNumberEditText.text.toString())
      patientRepository
          .save(ongoingEntry)
          .subscribeOn(io())
          .observeOn(mainThread())
          .subscribe({
            screenRouter.push(PatientPersonalDetailsEntryScreen.KEY)
          })
    })
  }

  private fun setupPatientSearchResults() {
    val resultsAdapter = PatientSearchResultsAdapter()

    patientRecyclerView.adapter = resultsAdapter
    patientRecyclerView.layoutManager = LinearLayoutManager(context)

    RxTextView.textChanges(mobileNumberEditText)
        .switchMap { searchQuery ->
          patientRepository
              .search(searchQuery.toString())
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
        }
        .takeUntil(RxView.detaches(this))
        .subscribe(resultsAdapter)
  }
}
