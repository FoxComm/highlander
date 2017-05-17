package payloads

import java.time.Instant
import models.payment.InStorePaymentStates
import models.payment.creditcard.CreditCardCharge
import org.json4s.JsonAST._
import org.scalacheck.Gen
import org.scalatest.prop.PropertyChecks
import payloads.EntityExportPayloads.FieldCalculation
import testutils.TestBase
import utils.Strings._

class EntityExportPayloadsTest extends TestBase with PropertyChecks {
  "FieldCalculation.State" - {
    val Active   = "\"Active\""
    val Inactive = "\"Inactive\""
    val now      = Instant.now()

    "should be active if current date is between activeFrom and activeTo" in {
      val obj = JObject("activeFrom" → JString(now.minusSeconds(10).toString),
                        "activeTo"             → JString(now.plusSeconds(60).toString))
      FieldCalculation.State.calculate("state" → obj) must === (Active)
    }

    "should be active if current date is after activeFrom and activeTo is null" in {
      val obj = JObject("activeFrom" → JString(now.minusSeconds(10).toString), "activeTo" → JNull)
      FieldCalculation.State.calculate("state" → obj) must === (Active)
    }

    "should be inactive if archiveAt is defined" in {
      val obj = JObject("activeFrom" → JString(now.minusSeconds(10).toString),
                        "activeTo"             → JString(now.plusSeconds(60).toString),
                        "archivedAt"           → JString(now.toString))
      FieldCalculation.State.calculate("state" → obj) must === (Inactive)
    }

    "should be inactive if activeFrom is null" in {
      val obj1 = JObject("activeTo"              → JString(now.plusSeconds(60).toString))
      val obj2 = obj1.merge(JObject("activeFrom" → JNull))

      FieldCalculation.State.calculate("state" → obj1) must === (Inactive)
      FieldCalculation.State.calculate("state" → obj2) must === (Inactive)
    }

    "should react only to state field" in {
      forAll(Gen.alphaStr.suchThat(_ != "state")) { field ⇒
        FieldCalculation.State.calculate.isDefinedAt(field → JObject()) must === (false)
      }
    }
  }

  "FieldCalculation.PaymentState" - {
    import models.cord.CordPaymentState._

    "should return 'Cart' state if payments are empty" in {
      FieldCalculation.PaymentState
        .calculate("payment.state" → JObject("payments" → JArray(Nil))) must === (
          Cart.toString.prettify.quote())
    }

    "should return 'Cart' if any state in payments is in cart state" in {
      val payments = List(
          JObject("giftCardState" → JString(
                  InStorePaymentStates.State.show(InStorePaymentStates.Capture))),
          JObject("creditCardState" → JString(
                  CreditCardCharge.State.show(CreditCardCharge.FailedAuth)))
      )
      FieldCalculation.PaymentState.calculate(
          "payment.state" → JObject("payments" → JArray(payments))) must === (
          Cart.toString.prettify.quote())
    }

    "should return 'Cart' if any state in payments is unknown" in {
      val payments = List(JObject("creditCardState" → JString("whatever")))
      FieldCalculation.PaymentState.calculate(
          "payment.state" → JObject("payments" → JArray(payments))) must === (
          Cart.toString.prettify.quote())
    }

    "should return 'Full Capture' if all payments are fully captured" in {
      val payments = List(
          JObject("storeCreditState" → JString(
                  InStorePaymentStates.State.show(InStorePaymentStates.Capture))),
          JObject("creditCardState" → JString(
                  CreditCardCharge.State.show(CreditCardCharge.FullCapture)))
      )
      FieldCalculation.PaymentState.calculate(
          "payment.state" → JObject("payments" → JArray(payments))) must === (
          FullCapture.toString.prettify.quote())
    }

    "should return 'Expired Auth' if any payment is expired" in {
      val payments = List(
          JObject("storeCreditState" → JString(
                  InStorePaymentStates.State.show(InStorePaymentStates.Capture))),
          JObject("creditCardState" → JString(
                  CreditCardCharge.State.show(CreditCardCharge.ExpiredAuth)))
      )
      FieldCalculation.PaymentState.calculate(
          "payment.state" → JObject("payments" → JArray(payments))) must === (
          ExpiredAuth.toString.prettify.quote())
    }

    "should return 'Failed Capture' if any payment has failed during capture" in {
      val payments = List(
          JObject("giftCardState" → JString(
                  InStorePaymentStates.State.show(InStorePaymentStates.Capture))),
          JObject("creditCardState" → JString(
                  CreditCardCharge.State.show(CreditCardCharge.FailedCapture)))
      )
      FieldCalculation.PaymentState.calculate(
          "payment.state" → JObject("payments" → JArray(payments))) must === (
          FailedCapture.toString.prettify.quote())
    }

    "should return 'Auth' if all payments are in auth state" in {
      val payments = List(
          JObject("giftCardState" → JString(
                  InStorePaymentStates.State.show(InStorePaymentStates.Auth))),
          JObject("creditCardState" → JString(CreditCardCharge.State.show(CreditCardCharge.Auth))),
          JObject("storeCreditState" → JString(
                  InStorePaymentStates.State.show(InStorePaymentStates.Auth)))
      )
      FieldCalculation.PaymentState.calculate(
          "payment.state" → JObject("payments" → JArray(payments))) must === (
          Auth.toString.prettify.quote())
    }
  }
}
