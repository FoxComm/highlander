package models.product

import models.objects._
import utils.IlluminateAlgorithm
import utils.aliases._

case class IlluminatedVariant(id: Int, context: IlluminatedContext, attributes: Json)

object IlluminatedVariant {

  def illuminate(c: ObjectContext, v: FullObject[ProductOption]): IlluminatedVariant =
    IlluminatedVariant(
        id = v.form.id,
        context = IlluminatedContext(c.name, c.attributes),
        attributes = IlluminateAlgorithm.projectAttributes(v.form.attributes, v.shadow.attributes))
}
