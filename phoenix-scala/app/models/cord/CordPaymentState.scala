package models.cord

import cats.implicits._
import com.pellucid.sealerate
import models.payment.{ExternalCharge, InStorePaymentStates}
import models.payment.applepay.ApplePayCharge
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

  def fromExternalState(ccPaymentState: ExternalCharge.State): State = {
    import models.payment.{ExternalCharge ⇒ exState}

    ccPaymentState match {
      case exState.Auth          ⇒ Auth
      case exState.ExpiredAuth   ⇒ ExpiredAuth
      case exState.FullCapture   ⇒ FullCapture
      case exState.FailedCapture ⇒ FailedCapture
      case _                     ⇒ Cart
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
