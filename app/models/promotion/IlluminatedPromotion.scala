package models.promotion

import models.objects._
import utils.IlluminateAlgorithm
import models.objects._
import models.Aliases.Json
import org.json4s.JsonDSL._
import org.json4s.JsonAST.{JString, JObject, JField, JNothing}

import java.time.Instant

/**
 * An IlluminatedPromotion is what you get when you combine the promotion shadow and
 * the form. 
 */
final case class IlluminatedPromotion(id: Int, context: IlluminatedContext, 
  attributes: Json)

object IlluminatedPromotion { 

  def illuminate(context: ObjectContext, promotion: Promotion, 
    form: ObjectForm, shadow: ObjectShadow) : IlluminatedPromotion = { 

    IlluminatedPromotion(
      id = form.id,  //Id points to form since that is constant across contexts
      context = IlluminatedContext(context.name, context.attributes),
      attributes = IlluminateAlgorithm.projectAttributes(form.attributes, shadow.attributes))
  }
}

