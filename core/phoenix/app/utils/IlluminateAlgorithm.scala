package utils

import java.time.Instant

import cats.data.Xor
import failures._
import failures.ObjectFailures._
import org.json4s.JsonAST.{JNothing, JNull, JObject, JString}
import org.json4s.JsonDSL._
import utils.aliases._

object IlluminateAlgorithm {
  implicit val formats = JsonFormatters.phoenixFormats

  val validateInstantFields = Set("activeFrom", "activeTo")

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

  def validateAttributesFish(formJson: Json, shadowJson: Json): Failures Xor None.type = {
    val result = validateAttributes(formJson, shadowJson)

    Failures(result: _*) match {
      case None           ⇒ Xor.right(None)
      case Some(failures) ⇒ Xor.left(failures)
    }
  }

  def validateTypes(formJson: Json, shadowJson: Json): Seq[Failure] = {
    (formJson, shadowJson) match {
      case (JObject(form), JObject(shadow)) ⇒
        shadow.obj.flatMap {
          case (attr, link) ⇒
            val ref = link \ "ref"
            ref match {
              case JString(key) ⇒ validateAttributeType(attr, key, formJson)
              case _            ⇒ Seq.empty[Failure]
            }
        }
      case _ ⇒ Seq.empty[Failure]
    }
  }

  def validateTypesFish(formJson: Json, shadowJson: Json): Failures Xor None.type = {
    val result = validateTypes(formJson: Json, shadowJson: Json)

    Failures(result: _*) match {
      case None           ⇒ Xor.right(None)
      case Some(failures) ⇒ Xor.left(failures)
    }
  }

  private def validateAttributeType(attr: String, key: String, form: Json): Seq[Failure] = {
    val value = form \ key

    if (validateInstantFields(attr) && value != JNull &&
        value != JNothing && value.extractOpt[Instant].isEmpty)
      Seq(ShadowAttributeInvalidTime(attr, value.toString))
    else
      Seq.empty[Failure]
  }

  private def validateAttribute(attr: String, key: String, form: Json): Seq[Failure] = {
    val value = form \ key

    val shadowAttributesErrors = value match {
      case JNothing ⇒ Seq(ShadowHasInvalidAttribute(attr, key))
      case _        ⇒ Seq.empty[Failure]
    }

    validateAttributeType(attr, key, form) ++ shadowAttributesErrors
  }
}
