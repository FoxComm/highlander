package models.product

import models.objects._

case class VariantValueMapping(variantShadowId: Int, value: FullObject[VariantValue])
