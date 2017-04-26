package models.payment

import com.pellucid.sealerate
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import utils.ADT

abstract class PaymentMethod {}

object PaymentMethod {
  sealed trait Payment {
    val isExternal = false
    val isInternal = false
  }

  sealed trait InternalPayment extends Payment {
    override val isInternal = true
  }

  sealed trait ExternalPayment extends Payment {
    override val isExternal = true
  }

  sealed trait Type extends Product with Serializable with Payment

  case object GiftCard    extends Type with InternalPayment
  case object StoreCredit extends Type with InternalPayment
  case object CreditCard  extends Type with ExternalPayment
  case object ApplePay    extends Type with ExternalPayment

  implicit object Type extends ADT[Type] {
    def types = sealerate.values[Type]
  }

  implicit val paymentMethodTypeColumnType: JdbcType[Type] with BaseTypedType[Type] =
    Type.slickColumn
}
