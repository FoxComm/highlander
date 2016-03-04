package utils

import services.Failure
import services.IlluminateFailure._
import org.json4s.JsonDSL._
import org.json4s.JsonAST.{JValue, JString, JObject, JField, JNothing}

object IlluminateAlgorithm { 

  def projectAttributes(form: JValue, shadow: JValue) : JValue = {
    shadow match {
      case JObject(s) ⇒  form match {
        case JObject(f) ⇒ 
          s.obj.map {
            case (attr, JString(key)) ⇒  (attr, findAttribute(attr, key, f))
            case (attr, _) ⇒  (attr, JNothing)
          }
        case _ ⇒ JNothing
      }
      case _ ⇒  JNothing
    }
  }

  def validateAttributes(form: JValue, shadow: JValue) : Seq[Failure] = {
    shadow match {
      case JObject(s) ⇒  form match {
        case JObject(f) ⇒ 
          s.obj.flatMap {
            case (attr, JString(key)) ⇒  validateAttribute(attr, key, f)
            case (attr, _) ⇒  Seq(ShadowAttributeNotAString(attr))
          }
        case _ ⇒ Seq(AttributesAreEmpty())
      }
      case _ ⇒  Seq(ShadowAttributesAreEmpty())
    }
  }

  private def findAttribute(attr: String, key: String, form: JObject) : JValue = {
    val t = form \ attr \ "type"
    val v = form \ attr \ key 
    ("t" → t) ~ ("v" → v)
  }

  private def validateAttribute(attr: String, key: String, form: JObject) : Seq[Failure] = {
    form \ attr \ key match {
      case JNothing ⇒  Seq(ShadowHasInvalidAttribute(attr, key))
      case v ⇒  Seq.empty
    }
  }
}

