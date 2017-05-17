package phoenix.models

import slick.ast.BaseTypedType
import slick.jdbc.JdbcType

package object inventory {
  implicit val skuTypeColumnType: JdbcType[SkuType] with BaseTypedType[SkuType] =
    SkuType.slickColumn
}
