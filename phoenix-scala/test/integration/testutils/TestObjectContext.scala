package testutils

import models.objects.{ObjectContext, ObjectContexts}
import models.product.SimpleContext
import org.scalatest.{Suite, SuiteMixin}

trait TestObjectContext extends SuiteMixin with GimmeSupport with DbTestSupport { this: Suite ⇒

  implicit val database                = db
  implicit lazy val ctx: ObjectContext = ObjectContexts.findOneById(SimpleContext.id).gimme.get
}
