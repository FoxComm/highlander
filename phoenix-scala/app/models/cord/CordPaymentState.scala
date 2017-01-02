package models.cord

import cats.implicits._
import com.pellucid.sealerate
import models.payment.InStorePaymentStates
import models.payment.creditcard.CreditCardCharge
import utils.ADT

object CordPaymentState {
  sealed trait State
  case object Auth          extends State
  case object Cart          extends State
  case object FullCapture   extends State
  case object FailedCapture extends State
  case object ExpiredAuth   extends State

  object State extends ADT[State] {
    def types = sealerate.values[State]
  }

  def fromInStoreState(inStorePaymentState: InStorePaymentStates.State): State = {
    import models.payment.{InStorePaymentStates ⇒ InStore}

    inStorePaymentState match {
      case InStore.Auth    ⇒ Auth
      case InStore.Capture ⇒ FullCapture
      case _               ⇒ Cart
    }
  }

  def fromCCState(ccPaymentState: CreditCardCharge.State): State = {
    import models.payment.creditcard.{CreditCardCharge ⇒ CC}

    ccPaymentState match {
      case CC.Auth          ⇒ Auth
      case CC.ExpiredAuth   ⇒ ExpiredAuth
      case CC.FullCapture   ⇒ FullCapture
      case CC.FailedCapture ⇒ FailedCapture
      case _                ⇒ Cart
    }
  }

  implicit class StateTests(payStates: Seq[State]) {
    def deduceCordPaymentState: State =
      fullyCaptured orElse expiredAuth orElse failedCapture getOrElse Auth

    private def fullyCaptured = if (payStates.forall(_ == FullCapture)) FullCapture.some else None
    private def expiredAuth   = payStates.find(_ == ExpiredAuth)
    private def failedCapture = payStates.find(_ == FailedCapture)
  }
}
