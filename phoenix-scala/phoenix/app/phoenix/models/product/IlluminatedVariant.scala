package phoenix.models.product

import objectframework.IlluminateAlgorithm
import objectframework.models._
import phoenix.utils.aliases._

case class IlluminatedVariant(id: Int, context: IlluminatedContext, attributes: Json)

object IlluminatedVariant {

  def illuminate(c: ObjectContext, v: FullObject[Variant]): IlluminatedVariant =
    IlluminatedVariant(id = v.form.id,
                       context = IlluminatedContext(c.name, c.attributes),
                       attributes =
                         IlluminateAlgorithm.projectAttributes(v.form.attributes, v.shadow.attributes))
}
