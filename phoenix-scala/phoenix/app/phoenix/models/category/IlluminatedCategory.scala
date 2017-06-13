package phoenix.models.category

import objectframework.IlluminateAlgorithm
import objectframework.models._
import phoenix.utils.aliases._

/**
  * An IlluminatedCategory is what you get when you combine the category shadow and
  * the form.
  */
case class IlluminatedCategory(id: Int, context: IlluminatedContext, attributes: Json)

object IlluminatedCategory {

  def illuminate(context: ObjectContext,
                 product: Category,
                 form: ObjectForm,
                 shadow: ObjectShadow): IlluminatedCategory = {
    val illuminatedContext = IlluminatedContext(context.name, context.attributes)
    val attributes         = IlluminateAlgorithm.projectAttributes(form.attributes, shadow.attributes)
    IlluminatedCategory(form.id, illuminatedContext, attributes)
  }
}
