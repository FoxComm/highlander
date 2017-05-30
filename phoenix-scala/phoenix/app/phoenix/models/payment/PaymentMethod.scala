package phoenix.models.payment

import com.pellucid.sealerate
import phoenix.utils.ADT
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType

abstract class PaymentMethod {}

object PaymentMethod {
  sealed trait PaymentTag {
    def isExternal: Boolean = false
    def isInternal: Boolean = false
  }

  sealed trait InternalPayment extends PaymentTag {
    override def isInternal: Boolean = true
  }

  sealed trait ExternalPayment extends PaymentTag {
    override def isExternal: Boolean = true
  }

  sealed trait Type extends Product with Serializable with PaymentTag

  case object GiftCard    extends Type with InternalPayment
  case object StoreCredit extends Type with InternalPayment
  case object CreditCard  extends Type with ExternalPayment
  case object ApplePay    extends Type with ExternalPayment

  implicit object Type extends ADT[Type] {
    def types            = sealerate.values[Type]
    def externalPayments = types.filter(_.isExternal)
    def internalPayments = types.filter(_.isInternal)
  }

  implicit val paymentMethodTypeColumnType: JdbcType[Type] with BaseTypedType[Type] =
    Type.slickColumn
}
