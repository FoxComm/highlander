package models.product

import models.Aliases.Json
import models.objects._
import utils.IlluminateAlgorithm

case class IlluminatedVariant(id: Int, context: IlluminatedContext, attributes: Json)

object IlluminatedVariant {

  def illuminate(c: ObjectContext, v: FullObject[Variant]): IlluminatedVariant =
    IlluminatedVariant(
        id = v.form.id,
        context = IlluminatedContext(c.name, c.attributes),
        attributes = IlluminateAlgorithm.projectAttributes(v.form.attributes, v.shadow.attributes))
}
