package models.promotion

import java.time.Instant

import cats.data.Xor
import cats.data.Xor._
import failures.PromotionFailures._
import failures._
import models.objects._
import models.promotion.Promotion._
import org.json4s.JsonAST.JNothing
import org.json4s.JsonDSL._
import utils.aliases._
import utils.{IlluminateAlgorithm, JsonFormatters}

/**
  * An IlluminatedPromotion is what you get when you combine the promotion shadow and
  * the form.
  */
case class IlluminatedPromotion(id: Int,
                                context: IlluminatedContext,
                                applyType: ApplyType,
                                attributes: Json) {

  implicit val formats = JsonFormatters.phoenixFormats

  def mustBeActive: Failures Xor IlluminatedPromotion = {
    val activeFrom = (attributes \ "activeFrom" \ "v").extractOpt[Instant]
    val activeTo   = (attributes \ "activeTo" \ "v").extractOpt[Instant]
    val now        = Instant.now

    (applyType, activeFrom, activeTo) match {
      case (Auto, Some(from), Some(to)) ⇒
        if (from.isBefore(now) && to.isAfter(now)) right(this)
        else Left(PromotionIsNotActive.single)
      case (Auto, Some(from), None) ⇒
        if (from.isBefore(now)) right(this) else Left(PromotionIsNotActive.single)
      case (Auto, _, _) ⇒
        Left(PromotionIsNotActive.single)
      case (Coupon, _, _) ⇒
        right(this)
    }
  }
}

object IlluminatedPromotion {

  def illuminate(context: ObjectContext, promotion: FullObject[Promotion]): IlluminatedPromotion =
    illuminate(context, promotion.model, promotion.form, promotion.shadow)

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

  def validatePromotion(applyType: ApplyType, promotion: FormAndShadow): FormAndShadow = {
    (applyType, promotion.getAttribute("activeFrom")) match {
      case (Promotion.Coupon, JNothing) ⇒
        promotion.setAttribute("activeFrom", "date", Instant.now.toString)
      case _ ⇒
        promotion
    }
  }
}
