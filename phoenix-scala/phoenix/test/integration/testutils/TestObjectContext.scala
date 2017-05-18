package testutils

import models.objects.{ObjectContext, ObjectContexts}
import models.product.SimpleContext
import org.scalatest.{SuiteMixin, TestSuite}

trait TestObjectContext extends SuiteMixin with GimmeSupport with DbTestSupport {
  this: TestSuite ⇒

  implicit val database                = db
  implicit lazy val ctx: ObjectContext = ObjectContexts.findOneById(SimpleContext.id).gimme.get
}
