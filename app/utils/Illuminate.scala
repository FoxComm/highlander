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
            case (attr, link) ⇒  {
              val t = link \ "type" 
              val ref = link \ "ref" 
              ref match {
                case JString(key) ⇒  (attr, (("t" → t) ~ ("v" → (form \ key))))
                case _ ⇒ (attr, JNothing)
              }
            }
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
            case (attr, link) ⇒  {
              val ref = link \ "ref" 
              ref match {
                case JString(key) ⇒ validateAttribute(attr, key, form)
                case _ ⇒ Seq(ShadowAttributeMissingRef(attr))
              }
            }
          }
        case _ ⇒ Seq(AttributesAreEmpty())
      }
      case _ ⇒  Seq(ShadowAttributesAreEmpty())
    }
  }

  private def validateAttribute(attr: String, key: String, form: JValue) : Seq[Failure] = {
    form \ key match {
      case JNothing ⇒  Seq(ShadowHasInvalidAttribute(attr, key))
      case v ⇒  Seq.empty
    }
  }
}

