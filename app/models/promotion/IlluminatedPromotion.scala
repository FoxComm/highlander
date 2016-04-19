package models.promotion

import models.Aliases.Json
import models.objects._
import utils.IlluminateAlgorithm

/**
 * An IlluminatedPromotion is what you get when you combine the promotion shadow and
 * the form. 
 */
case class IlluminatedPromotion(id: Int, context: IlluminatedContext, 
  applyType: Promotion.ApplyType, attributes: Json)

object IlluminatedPromotion { 

  def illuminate(context: ObjectContext, promotion: Promotion, 
    form: ObjectForm, shadow: ObjectShadow) : IlluminatedPromotion = { 

    IlluminatedPromotion(
      id = form.id,  //Id points to form since that is constant across contexts
      applyType = promotion.applyType,
      context = IlluminatedContext(context.name, context.attributes),
      attributes = IlluminateAlgorithm.projectAttributes(form.attributes, shadow.attributes))
  }
}

