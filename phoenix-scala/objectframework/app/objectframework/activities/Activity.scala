package objectframework.activities

import core.utils.snakeCaseName

trait Activity[A] { self: A ⇒
  def typeName: String = snakeCaseName(this)
}
