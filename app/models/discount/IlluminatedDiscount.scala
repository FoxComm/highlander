package models.discount

import models.objects._
import utils.IlluminateAlgorithm
import models.objects._
import models.Aliases.Json
import org.json4s.JsonDSL._
import org.json4s.JsonAST.{JString, JObject, JField, JNothing}

import java.time.Instant
import cats.implicits._

/**
 * An IlluminatedDiscount is what you get when you combine the discount shadow and
 * the form. 
 */
final case class IlluminatedDiscount(id: Int, context: Option[IlluminatedContext], 
  attributes: Json)

object IlluminatedDiscount { 

  def illuminate(context: Option[ObjectContext] = None, form: ObjectForm, shadow: ObjectShadow) : IlluminatedDiscount = { 

    IlluminatedDiscount(
      id = form.id,  //Id points to form since that is constant across contexts
      context = context match { 
        case Some(context) ⇒ IlluminatedContext(context.name, context.attributes).some
        case None ⇒ None
      },
      attributes = IlluminateAlgorithm.projectAttributes(form.attributes, shadow.attributes))
  }
}

