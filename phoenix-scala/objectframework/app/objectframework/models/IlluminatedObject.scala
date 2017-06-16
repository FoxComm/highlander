package objectframework.models

import core.db.Identity
import objectframework.IlluminateAlgorithm
import org.json4s.JValue

case class IlluminatedContext(name: String, attributes: JValue)

/**
  * An IlluminatedObject is what you get when you combine the product shadow and
  * the product.
  */
case class IlluminatedObject(id: Int = 0, attributes: JValue) extends Identity[IlluminatedObject]

object IlluminatedObject {

  def illuminate(form: ObjectForm, shadow: ObjectShadow): IlluminatedObject = {
    val attributes = IlluminateAlgorithm.projectAttributes(form.attributes, shadow.attributes)
    IlluminatedObject(id = form.id, attributes = attributes)
  }
}
