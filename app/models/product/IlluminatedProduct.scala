package models.product

import models.objects._
import utils.IlluminateAlgorithm
import models.objects._
import models.Aliases.Json
import org.json4s.JsonDSL._
import org.json4s.JsonAST.{JString, JObject, JField, JNothing}

import java.time.Instant

/**
 * An IlluminatedProduct is what you get when you combine the product shadow and
 * the form. 
 */
final case class IlluminatedProduct(id: Int, context: IlluminatedContext, 
  attributes: Json)

object IlluminatedProduct { 

  def illuminate(context: ObjectContext, product: Product, 
    form: ObjectForm, shadow: ObjectShadow) : IlluminatedProduct = { 

    IlluminatedProduct(
      id = form.id,  //Products should have a code like skus.
      context = IlluminatedContext(context.name, context.attributes),
      attributes = IlluminateAlgorithm.projectAttributes(form.attributes, shadow.attributes))
  }
}

