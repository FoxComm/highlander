package models

import com.pellucid.sealerate
import com.stripe.model.{Card ⇒ StripeCard}
import com.wix.accord.dsl.{validator ⇒ createValidator}
import com.wix.accord.{Failure ⇒ ValidationFailure}

import services.{Result, Failures, Failure}
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}

import scala.concurrent.{ExecutionContext, Future}

import utils.ADT

abstract class PaymentMethod {
  def authorize(amount: Int)(implicit ec: ExecutionContext): Result[String]
}

object PaymentMethod {
  sealed trait Type
  case object CreditCard extends Type
  case object GiftCard extends Type
  case object StoreCredit extends Type

  object Type extends ADT[Type] {
    def types = sealerate.values[Type]
  }

  implicit val paymentMethodTypeColumnType = Type.slickColumn
}

