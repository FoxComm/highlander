package testutils.fixtures

import services.account.AccountCreateContext

/**
  * Created by Alex Afanasev
  */
trait AccountContext {
  val customerContext = AccountCreateContext(List("customer"), "merchant", 1)

}
