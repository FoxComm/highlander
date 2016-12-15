package models.product

import models.objects._
import utils.IlluminateAlgorithm
import utils.aliases._

case class IlluminatedProductOption(id: Int, context: IlluminatedContext, attributes: Json)

object IlluminatedProductOption {

  def illuminate(c: ObjectContext, v: FullObject[ProductOption]): IlluminatedProductOption =
    IlluminatedProductOption(
        id = v.form.id,
        context = IlluminatedContext(c.name, c.attributes),
        attributes = IlluminateAlgorithm.projectAttributes(v.form.attributes, v.shadow.attributes))
}
