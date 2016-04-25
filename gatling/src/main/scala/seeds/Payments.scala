package seeds


import io.gatling.core.Predef._
import seeds.CreditCards._
import seeds.GiftCards._

object Payments {

  // Add payment sequences here
  val pay = uniformRandomSwitch(
    exec(payWithGc).exec(payWithCc),
    exec(payWithCc)
  )
}
