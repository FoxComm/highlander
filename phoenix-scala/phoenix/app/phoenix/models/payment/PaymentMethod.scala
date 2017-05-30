package phoenix.models.payment

import com.pellucid.sealerate
import core.ADT
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType

abstract class PaymentMethod {}

object PaymentMethod {
  sealed trait Type extends Product with Serializable
  case object CreditCard  extends Type
  case object GiftCard    extends Type
  case object StoreCredit extends Type

  implicit object Type extends ADT[Type] {
    def types = sealerate.values[Type]
  }

  implicit val paymentMethodTypeColumnType: JdbcType[Type] with BaseTypedType[Type] =
    Type.slickColumn
}
