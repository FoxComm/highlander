package models.product

import models.Aliases.Json
import models.objects._
import utils.IlluminateAlgorithm

/**
  * An IlluminatedProduct is what you get when you combine the product shadow and
  * the form. 
  */
case class IlluminatedProduct(id: Int, context: IlluminatedContext, attributes: Json)

object IlluminatedProduct {

  def illuminate(context: ObjectContext,
                 product: Product,
                 form: ObjectForm,
                 shadow: ObjectShadow): IlluminatedProduct = {

    IlluminatedProduct(id = form.id, //Products should have a code like skus.
                       context = IlluminatedContext(context.name, context.attributes),
                       attributes =
                         IlluminateAlgorithm.projectAttributes(form.attributes, shadow.attributes))
  }
}
