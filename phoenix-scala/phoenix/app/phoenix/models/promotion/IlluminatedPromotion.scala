package phoenix.models.promotion

import java.time.Instant

import cats.implicits._
import core.failures._
import objectframework.IlluminateAlgorithm
import objectframework.models._
import org.json4s.JsonAST.JNothing
import org.json4s.JsonDSL._
import phoenix.failures.PromotionFailures._
import phoenix.models.promotion.Promotion._
import phoenix.utils.JsonFormatters
import phoenix.utils.aliases._

/**
  * An IlluminatedPromotion is what you get when you combine the promotion shadow and
  * the form.
  */
case class IlluminatedPromotion(id: Int, context: IlluminatedContext, applyType: ApplyType, attributes: Json) {

  implicit val formats = JsonFormatters.phoenixFormats

  def mustBeActive: Either[Failures, IlluminatedPromotion] = {
    val activeFrom = (attributes \ "activeFrom" \ "v").extractOpt[Instant]
    val activeTo   = (attributes \ "activeTo" \ "v").extractOpt[Instant]
    val now        = Instant.now

    (activeFrom, activeTo) match {
      case (Some(from), Some(to)) ⇒
        if (from.isBefore(now) && to.isAfter(now)) Either.right(this)
        else Either.left(PromotionIsNotActive.single)
      case (Some(from), None) ⇒
        if (from.isBefore(now)) Either.right(this) else Either.left(PromotionIsNotActive.single)
      case (_, _) ⇒
        Either.left(PromotionIsNotActive.single)
    }
  }
}

object IlluminatedPromotion {

  def illuminate(context: ObjectContext,
                 promotion: Promotion,
                 form: ObjectForm,
                 shadow: ObjectShadow): IlluminatedPromotion =
    IlluminatedPromotion(
      id = form.id, //Id points to form since that is constant across contexts
      applyType = promotion.applyType,
      context = IlluminatedContext(context.name, context.attributes),
      attributes = IlluminateAlgorithm.projectAttributes(form.attributes, shadow.attributes)
    )

  def validatePromotion(applyType: ApplyType, promotion: FormAndShadow): FormAndShadow =
    (applyType, promotion.getAttribute("activeFrom")) match {
      case (Promotion.Coupon, JNothing) ⇒
        promotion.setAttribute("activeFrom", "date", Instant.now.toString)
      case _ ⇒
        promotion
    }
}
