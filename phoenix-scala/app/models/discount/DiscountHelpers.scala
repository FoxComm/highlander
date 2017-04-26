package models.discount

import io.circe._
import models.objects._

object DiscountHelpers {

  def offer(form: ObjectForm, shadow: ObjectShadow): Json = {
    ObjectUtils.get("offer", form, shadow).getOrElse(Json.fromJsonObject(JsonObject.empty))
  }

  def qualifier(form: ObjectForm, shadow: ObjectShadow): Json = {
    ObjectUtils.get("qualifier", form, shadow).getOrElse(Json.fromJsonObject(JsonObject.empty))
  }
}
