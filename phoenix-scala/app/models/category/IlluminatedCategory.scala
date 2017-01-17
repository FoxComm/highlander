package models.category

import models.objects._
import utils.IlluminateAlgorithm
import utils.aliases._

/**
  * An IlluminatedCategory is what you get when you combine the category shadow and
  * the form.
  */
case class IlluminatedCategory(id: Int, context: IlluminatedContext, attributes: Json)

object IlluminatedCategory {

  def illuminate(context: ObjectContext,
                 product: Category,
                 form: ObjectForm,
                 shadow: ObjectShadow) = {
    val illuminatedContext = IlluminatedContext(context.name, context.attributes)
    val attributes         = IlluminateAlgorithm.projectAttributes(form.attributes, shadow.attributes)
    IlluminatedCategory(form.id, illuminatedContext, attributes)
  }
}
