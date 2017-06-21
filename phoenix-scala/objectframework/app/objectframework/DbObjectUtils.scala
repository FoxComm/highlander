package objectframework

import objectframework.models.{ObjectForms, ObjectShadows}
import slick.lifted.Rep
import org.json4s.JValue
import core.db.ExPostgresDriver.api._

object DbObjectUtils {

  trait DbFormAndShadow {
    def form: ObjectForms
    def shadow: ObjectShadows

    // get illuminated value using database jsonb functions
    def |→(key: String): Rep[JValue] =
      form.attributes +> (shadow.attributes +> key +>> "ref")
  }

  implicit class DbObjectIlluminated(val pair: (ObjectForms, ObjectShadows)) extends DbFormAndShadow {
    override def form: ObjectForms     = pair._1
    override def shadow: ObjectShadows = pair._2
  }
}
