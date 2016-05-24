package models.discount

import models.objects._
import org.json4s.JsonAST.JValue

object DiscountHelpers {

  def offer(f: ObjectForm, s: ObjectShadow): JValue = {
    ObjectUtils.get("offer", f, s)
  }

  def qualifier(f: ObjectForm, s: ObjectShadow): JValue = {
    ObjectUtils.get("qualifier", f, s)
  }
}
