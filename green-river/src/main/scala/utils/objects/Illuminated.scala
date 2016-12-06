package utils.objects

import org.json4s.JsonAST._
import org.json4s.JsonDSL._
import org.json4s.{JValue ⇒ Json}

object Illuminated {
  def get(attr: String, form: Json, shadow: Json): Json =
    shadow \ attr \ "ref" match {
      case JString(key) ⇒ form \ key
      case _            ⇒ JNothing
    }
}
