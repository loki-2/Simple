package org.simple.clinic.storage.migrations

import org.junit.Test
import org.simple.clinic.assertViewExists

class Migration70AndroidTest : BaseDatabaseMigrationTest(69, 70) {

  @Test
  fun the_overdue_appointment_view_should_be_added() {
    before.assertViewExists("OverdueAppointment")
    after.assertViewExists("OverdueAppointment")
  }
}
