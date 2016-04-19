package models.coupon

import models.Aliases.Json
import models.objects._
import utils.IlluminateAlgorithm

/**
 * An IlluminatedCoupon is what you get when you combine the coupon shadow and
 * the form. 
 */
case class IlluminatedCoupon(id: Int, context: IlluminatedContext, 
  attributes: Json, promotion: Int)

object IlluminatedCoupon { 

  def illuminate(context: ObjectContext, coupon: Coupon, 
    form: ObjectForm, shadow: ObjectShadow) : IlluminatedCoupon = { 

    IlluminatedCoupon(
      id = coupon.formId,  
      promotion = coupon.promotionId,
      context = IlluminatedContext(context.name, context.attributes),
      attributes = IlluminateAlgorithm.projectAttributes(form.attributes, shadow.attributes))
  }
}
