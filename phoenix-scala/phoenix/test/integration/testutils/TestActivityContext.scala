package testutils

import com.github.tminglei.slickpg.LTree
import phoenix.models.activity.ActivityContext

object TestActivityContext {

  trait AdminAC {
    implicit val ac =
      ActivityContext(userId = 1, userType = "admin", scope = LTree(""), transactionId = "xxx")
  }

  trait CustomerAC {
    implicit val ac =
      ActivityContext(userId = 1, userType = "customer", scope = LTree(""), transactionId = "xxx")
  }
}
