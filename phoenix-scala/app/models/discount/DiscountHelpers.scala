package models.discount

import models.objects._
import utils.aliases._

object DiscountHelpers {

  def offer(form: ObjectForm, shadow: ObjectShadow): Json = {
    ObjectUtils.get("offer", form, shadow)
  }

  def qualifier(form: ObjectForm, shadow: ObjectShadow): Json = {
    ObjectUtils.get("qualifier", form, shadow)
  }
}
