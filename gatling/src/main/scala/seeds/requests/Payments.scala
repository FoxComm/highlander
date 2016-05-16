package seeds.requests

import io.gatling.core.Predef._
import seeds.requests.CreditCards._
import seeds.requests.GiftCards._

object Payments {

  // Add payment sequences here
  val pay = uniformRandomSwitch(
    exec(payWithGc).exec(payWithCc),
    exec(payWithCc)
  )
}
