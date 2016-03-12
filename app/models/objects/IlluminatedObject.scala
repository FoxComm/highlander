package models.objects

import utils.IlluminateAlgorithm
import models.Aliases.Json
import org.json4s.JsonDSL._
import org.json4s.JsonAST.{JString, JObject, JField, JNothing}
import java.time.Instant

final case class IlluminatedContext(name: String, attributes: Json)

/**
 * An IlluminatedObject is what you get when you combine the product shadow and
 * the product. 
 */
final case class IlluminatedObject(id: Int = 0, attributes: Json)

object IlluminatedObject { 

  def illuminate(form: ObjectForm, shadow: ObjectShadow) : IlluminatedObject = { 
    IlluminatedObject(id = form.id, 
      attributes = IlluminateAlgorithm.projectAttributes(form.attributes, shadow.attributes))
  }
}

