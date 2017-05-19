package gatling.seeds.requests

import io.gatling.core.Predef._
import gatling.seeds.requests.CreditCards._
import gatling.seeds.requests.GiftCards._

object Payments {

  // Add payment sequences here
  val pay = uniformRandomSwitch(
      exec(payWithGc).exec(payWithCc),
      exec(payWithCc)
  )
}
