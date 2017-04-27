import models.activity.Activities
import testutils._

class ActivityTrailIntegrationTest extends IntegrationTestBase {

  "activity id is taken from the database" in {
    val firstId  = Activities.nextActivityId().gimme
    val secondId = Activities.nextActivityId().gimme
    secondId must === (firstId + 1)
  }
}
