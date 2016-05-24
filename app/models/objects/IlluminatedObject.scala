package models.objects

import models.Aliases.Json
import utils.IlluminateAlgorithm

case class IlluminatedContext(name: String, attributes: Json)

/**
  * An IlluminatedObject is what you get when you combine the product shadow and
  * the product. 
  */
case class IlluminatedObject(id: Int = 0, attributes: Json)

object IlluminatedObject {

  def illuminate(form: ObjectForm, shadow: ObjectShadow): IlluminatedObject = {
    IlluminatedObject(id = form.id,
                      attributes =
                        IlluminateAlgorithm.projectAttributes(form.attributes, shadow.attributes))
  }
}
