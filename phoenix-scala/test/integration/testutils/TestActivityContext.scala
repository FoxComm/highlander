package testutils

import models.activity.ActivityContext

object TestActivityContext {

  trait AdminAC {
    implicit val ac = ActivityContext.build(userId = 1, userType = "admin")
  }

  trait CustomerAC {
    implicit val ac = ActivityContext.build(userId = 1, userType = "customer")
  }
}
