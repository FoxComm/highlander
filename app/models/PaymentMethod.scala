package models

import com.pellucid.sealerate
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import utils.ADT

abstract class PaymentMethod {}

object PaymentMethod {
  sealed trait Type
  case object CreditCard extends Type
  case object GiftCard extends Type
  case object StoreCredit extends Type

  object Type extends ADT[Type] {
    def types = sealerate.values[Type]
  }

  implicit val paymentMethodTypeColumnType: JdbcType[Type] with BaseTypedType[Type] = Type.slickColumn
}

