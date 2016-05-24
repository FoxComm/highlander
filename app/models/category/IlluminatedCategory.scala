package models.category

import models.Aliases.Json
import models.objects._
import utils.IlluminateAlgorithm

/**
 * An IlluminatedCategory is what you get when you combine the category shadow and
 * the form. 
 */
case class IlluminatedCategory(id: Int, context: IlluminatedContext, attributes: Json)

object IlluminatedCategory {

  def illuminate(context: ObjectContext, product: Category, form: ObjectForm, shadow: ObjectShadow) =
    IlluminatedCategory(form.id, IlluminatedContext(context.name, context.attributes),
      IlluminateAlgorithm.projectAttributes(form.attributes, shadow.attributes))
}

