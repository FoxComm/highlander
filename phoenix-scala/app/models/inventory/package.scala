package models

import slick.ast.BaseTypedType
import slick.jdbc.JdbcType

package object inventory {
  implicit val productVariantTypeColumnType: JdbcType[ProductVariantType] with BaseTypedType[
      ProductVariantType] = ProductVariantType.slickColumn
}
