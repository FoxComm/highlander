package utils.db

import models.objects.{ObjectForms, ObjectShadows}
import slick.lifted.Rep
import utils.aliases.Json
import utils.db.ExPostgresDriver.api._

object DbObjectUtils {

  trait DbFormAndShadow {
    def form: ObjectForms
    def shadow: ObjectShadows

    def |→(key: String): Rep[Json] = {
      form.attributes +> (shadow.attributes +> key +>> "ref")
    }
  }

  implicit class DbObjectIlluminated(val pair: (ObjectForms, ObjectShadows))
      extends DbFormAndShadow {
    override def form: ObjectForms     = pair._1
    override def shadow: ObjectShadows = pair._2
  }
}
