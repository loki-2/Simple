package org.simple.mobius.migration

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.junit.Test
import org.simple.mobius.migration.fix.EveModel
import org.simple.mobius.migration.fix.defaultModel
import org.simple.mobius.migration.fix.eveEffectHandler
import org.simple.mobius.migration.fix.eveInit
import org.simple.mobius.migration.fix.eveUpdate

class EventsOnlyTest {
  @Test
  fun `it can run a state machine that's driven without external events`() {
    // given
    val modelUpdatesSubject = PublishSubject.create<EveModel>()
    val testObserver = modelUpdatesSubject.test()

    val fixture = MobiusTestFixture(
        Observable.empty(),
        defaultModel,
        ::eveInit,
        ::eveUpdate,
        eveEffectHandler(),
        modelUpdatesSubject::onNext
    )

    // then
    testObserver
        .assertNoErrors()
        .assertValues('a', 'b', 'c')
        .assertNotTerminated()

    fixture.dispose()
  }
}
