package objectframework

import java.time.Instant

import cats.data.NonEmptyList
import com.networknt.schema.JsonSchemaFactory
import com.typesafe.scalalogging.LazyLogging
import core.db._
import core.failures.Failure
import objectframework.ObjectFailures._
import objectframework.models._
import org.json4s.Formats
import org.json4s.JsonAST._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods.asJsonNode

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext

object IlluminateAlgorithm extends LazyLogging {

  def get(attr: String, form: JValue, shadow: JValue): JValue = shadow \ attr \ "ref" match {
    case JString(key) ⇒ form \ key
    case _            ⇒ JNothing
  }

  private def getInternalAttributes(schema: ObjectFullSchema): Option[JObject] =
    schema.schema \ "properties" \ "attributes" match {
      case JObject(s) ⇒ Some(s)
      case _          ⇒ None
    }

  def validateObjectBySchema(schema: ObjectFullSchema, form: ObjectForm, shadow: ObjectShadow)(
      implicit ec: ExecutionContext): DbResultT[JValue] = {
    val illuminated = projectFlatAttributes(form.attributes, shadow.attributes)
    getInternalAttributes(schema).fold {
      DbResultT.good(illuminated)
    } { jsonSchema ⇒
      val jsonSchemaFactory = new JsonSchemaFactory
      val validator         = jsonSchemaFactory.getSchema(asJsonNode(jsonSchema))

      val errorMessages = validator.validate(asJsonNode(illuminated)).asScala.toList

      errorMessages.map { err ⇒
        ObjectValidationFailure(form.kind, shadow.id, err.getMessage)
      } match {
        case head :: tail ⇒ DbResultT.failures[JValue](NonEmptyList(head, tail))
        case Nil          ⇒ DbResultT.good(illuminated)
      }
    }
  }

  def projectAttributes(formJson: JValue, shadowJson: JValue): JValue =
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

  def projectFlatAttributes(formJson: JValue, shadowJson: JValue): JValue =
    (formJson, shadowJson) match {
      case (JObject(from), JObject(shadow)) ⇒
        shadow.obj.map {
          case (attr, link) ⇒
            def ref = link \ "ref"
            ref match {
              case JString(key) ⇒ (attr, formJson \ key)
              case _            ⇒ (attr, JNothing)
            }
        }
      case _ ⇒
        JNothing
    }

  def validateAttributes(formJson: JValue, shadowJson: JValue)(implicit fmt: Formats): Seq[Failure] =
    (formJson, shadowJson) match {
      case (JObject(form), JObject(shadow)) ⇒
        shadow.obj.flatMap {
          case (attr, link) ⇒
            val ref = link \ "ref"
            val t   = link \ "type"
            ref match {
              case JString(key) ⇒ validateAttribute(attr, key, t, formJson)
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

  def validateAttributesTypes(formJson: JValue, shadowJson: JValue)(implicit fmt: Formats): Seq[Failure] =
    (formJson, shadowJson) match {
      case (JObject(form), JObject(shadow)) ⇒
        shadow.obj.flatMap {
          case (attr, link) ⇒
            val typed = link \ "type"
            val ref   = link \ "ref"
            ref match {
              case JString(key) ⇒ validateAttributeType(attr, key, typed, formJson)
              case _            ⇒ Seq.empty[Failure]
            }
        }
      case _ ⇒ Seq.empty[Failure]
    }

  private def validateAttributeType(attr: String, key: String, typed: JValue, form: JValue)(
      implicit fmt: Formats): Seq[Failure] = {
    val value = form \ key

    typed match {
      case JString("datetime") ⇒
        if (value != JNull && value != JNothing && value.extractOpt[Instant].isEmpty)
          Seq(ShadowAttributeInvalidTime(attr, value.toString))
        else
          Seq.empty[Failure]
      case _ ⇒ Seq.empty[Failure]
    }
  }

  private def validateAttribute(attr: String, key: String, typed: JValue, form: JValue)(
      implicit fmt: Formats): Seq[Failure] = {
    val value = form \ key

    val shadowAttributesErrors = value match {
      case JNothing ⇒ Seq(ShadowHasInvalidAttribute(attr, key))
      case _        ⇒ Seq.empty[Failure]
    }

    validateAttributeType(attr, key, typed, form) ++ shadowAttributesErrors
  }

}
