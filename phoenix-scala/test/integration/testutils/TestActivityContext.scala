package testutils

import com.github.tminglei.slickpg.LTree
import models.activity.ActivityContext

object TestActivityContext {

  trait AdminAC {
    implicit val ac = ActivityContext.build(userId = 1, userType = "admin", scope = LTree(""))
  }

  trait CustomerAC {
    implicit val ac = ActivityContext.build(userId = 1, userType = "customer", scope = LTree(""))
  }
}
