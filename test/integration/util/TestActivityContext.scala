package util

import faker.Lorem.letterify
import models.activity.ActivityContext

object TestActivityContext {

  trait AdminAC {
    implicit val ac = ActivityContext(userId = 1, userType = "admin", transactionId = randomId)
  }

  trait CustomerAC {
    implicit val ac = ActivityContext(userId = 1, userType = "customer", transactionId = randomId)
  }

  private def randomId = letterify("?" * 5)
}
