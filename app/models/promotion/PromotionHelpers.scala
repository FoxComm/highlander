package models.promotion

import models.objects._
import org.json4s.JsonAST.JValue

object PromotionHelpers { 

  def name(f: ObjectForm, s: ObjectShadow) : JValue = {
    ObjectUtils.get("name", f, s)
  }

  def description(f: ObjectForm, s: ObjectShadow) : JValue = {
    ObjectUtils.get("description", f, s)
  }
}
