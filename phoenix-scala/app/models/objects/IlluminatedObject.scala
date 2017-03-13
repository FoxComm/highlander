package models.objects

import utils.IlluminateAlgorithm
import utils.aliases._
import utils.db.Identity

case class IlluminatedContext(name: String, attributes: Json)

/**
  * An IlluminatedObject is what you get when you combine the product shadow and
  * the product. 
  */
case class IlluminatedObject(id: Int = 0, kind: String, attributes: Json)
    extends Identity[IlluminatedObject]

object IlluminatedObject {

  def illuminate(form: ObjectForm, shadow: ObjectShadow): IlluminatedObject = {
    val attributes = IlluminateAlgorithm.projectAttributes(form.attributes, shadow.attributes)
    IlluminatedObject(id = form.id, kind = form.kind, attributes = attributes)
  }
}
