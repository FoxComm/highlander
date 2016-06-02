package utils

import failures.Failure
import failures.ObjectFailures._
import org.json4s.JsonAST.{JNothing, JObject, JString}
import org.json4s.JsonDSL._
import utils.aliases._

object IlluminateAlgorithm {

  def get(attr: String, form: Json, shadow: Json): Json = shadow \ attr \ "ref" match {
    case JString(key) ⇒ form \ key
    case _            ⇒ JNothing
  }

  def projectAttributes(formJson: Json, shadowJson: Json): Json =
    (formJson, shadowJson) match {
      case (JObject(from), JObject(shadow)) ⇒
        shadow.obj.map {
          case (attr, link) ⇒
            def typed = link \ "type"
            def ref   = link \ "ref"
            ref match {
              case JString(key) ⇒ (attr, ("t" → typed) ~ ("v" → (formJson \ key)))
              case _            ⇒ (attr, JNothing)
            }
        }
      case _ ⇒
        JNothing
    }

  def validateAttributes(formJson: Json, shadowJson: Json): Seq[Failure] =
    (formJson, shadowJson) match {
      case (JObject(form), JObject(shadow)) ⇒
        shadow.obj.flatMap {
          case (attr, link) ⇒
            val ref = link \ "ref"
            ref match {
              case JString(key) ⇒ validateAttribute(attr, key, formJson)
              case _            ⇒ Seq(ShadowAttributeMissingRef(attr))
            }
        }
      case (JObject(_), _) ⇒
        Seq(ShadowAttributesAreEmpty)
      case (_, JObject(_)) ⇒
        Seq(FormAttributesAreEmpty)
      case _ ⇒
        Seq(FormAttributesAreEmpty, ShadowAttributesAreEmpty)
    }

  private def validateAttribute(attr: String, key: String, form: Json): Seq[Failure] =
    form \ key match {
      case JNothing ⇒ Seq(ShadowHasInvalidAttribute(attr, key))
      case _        ⇒ Seq.empty
    }
}
