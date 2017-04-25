package phoenix.models.payment

import com.pellucid.sealerate
import phoenix.utils.ADT
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType

abstract class PaymentMethod {}

object PaymentMethod {
  sealed trait Internal
  sealed trait External

  sealed trait Type extends Product with Serializable {
    def isExternal: Boolean = this.isInstanceOf[External]
    def isInternal: Boolean = this.isInstanceOf[Internal]
  }

  case object GiftCard    extends Type with Internal
  case object StoreCredit extends Type with Internal
  case object CreditCard  extends Type with External
  case object ApplePay    extends Type with External

  implicit object Type extends ADT[Type] {
    def types = sealerate.values[Type]
  }

  implicit val paymentMethodTypeColumnType: JdbcType[Type] with BaseTypedType[Type] =
    Type.slickColumn
}
