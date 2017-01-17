package models.discount

import models.objects._
import utils.IlluminateAlgorithm
import utils.aliases._

/**
  * An IlluminatedDiscount is what you get when you combine the discount shadow and
  * the form.
  */
case class IlluminatedDiscount(id: Int, context: Option[IlluminatedContext], attributes: Json)

object IlluminatedDiscount {

  def illuminate(context: Option[ObjectContext] = None,
                 form: ObjectForm,
                 shadow: ObjectShadow): IlluminatedDiscount = {

    IlluminatedDiscount(
      id = form.id, //Id points to form since that is constant across contexts
      context = context.map(c ⇒ IlluminatedContext(c.name, c.attributes)),
      attributes = IlluminateAlgorithm.projectAttributes(form.attributes, shadow.attributes))
  }
}
