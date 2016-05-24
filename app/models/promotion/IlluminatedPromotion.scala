package models.promotion

import java.time.Instant

import cats.data.Xor
import cats.data.Xor._
import failures._
import failures.PromotionFailures._
import models.Aliases.Json
import models.objects._
import utils.{IlluminateAlgorithm, JsonFormatters}

/**
  * An IlluminatedPromotion is what you get when you combine the promotion shadow and
  * the form. 
  */
case class IlluminatedPromotion(
    id: Int, context: IlluminatedContext, applyType: Promotion.ApplyType, attributes: Json) {

  implicit val formats = JsonFormatters.phoenixFormats

  def mustBeActive: Failures Xor IlluminatedPromotion = {
    val activeFrom = (attributes \ "activeFrom" \ "v").extractOpt[Instant]
    val activeTo   = (attributes \ "activeTo" \ "v").extractOpt[Instant]
    val now        = Instant.now

    (activeFrom, activeTo) match {
      case (Some(from), Some(to)) ⇒
        if (from.isBefore(now) && to.isAfter(now)) right(this)
        else Left(PromotionIsNotActive.single)
      case (Some(from), None) ⇒
        if (from.isBefore(now)) right(this) else Left(PromotionIsNotActive.single)
      case (_, _) ⇒
        Left(PromotionIsNotActive.single)
    }
  }
}

object IlluminatedPromotion {

  def illuminate(context: ObjectContext,
                 promotion: Promotion,
                 form: ObjectForm,
                 shadow: ObjectShadow): IlluminatedPromotion = {

    IlluminatedPromotion(
        id = form.id, //Id points to form since that is constant across contexts
        applyType = promotion.applyType,
        context = IlluminatedContext(context.name, context.attributes),
        attributes = IlluminateAlgorithm.projectAttributes(form.attributes, shadow.attributes))
  }
}
