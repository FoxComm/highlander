package models

import slick.ast.BaseTypedType
import slick.jdbc.JdbcType

package object inventory {
  implicit val skuTypeColumnType: JdbcType[VariantType] with BaseTypedType[VariantType] =
    VariantType.slickColumn
}
