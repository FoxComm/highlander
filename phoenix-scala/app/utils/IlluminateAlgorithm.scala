package utils

import java.time.Instant

import cats.data._
import com.typesafe.scalalogging.LazyLogging
import failures.Failure
import failures.ObjectFailures._
import models.objects._
import org.json4s.JsonAST._
import org.json4s.JsonDSL._
import utils.aliases._
import utils.db._

// json schema
import scala.collection.JavaConverters._
import com.networknt.schema.JsonSchemaFactory
import org.json4s.jackson.JsonMethods.asJsonNode

object IlluminateAlgorithm extends LazyLogging {

  def isActive(attributes: Map[String, Json]): Boolean = {
    implicit val formats = JsonFormatters.phoenixFormats

    def extractInstant(json: Json) = (json \ "v").extractOpt[Instant]
    def beforeNow(time: Instant)   = time.isBefore(Instant.now)

    val activeFrom = attributes.get("activeFrom").flatMap(extractInstant)
    val activeTo   = attributes.get("activeTo").flatMap(extractInstant)

    activeFrom.exists(beforeNow) && !activeTo.exists(beforeNow)
  }

  implicit val formats = JsonFormatters.phoenixFormats

  def get(attr: String, form: Json, shadow: Json): Json = shadow \ attr \ "ref" match {
    case JString(key) ⇒ form \ key
    case _            ⇒ JNothing
  }

  private def getInternalAttributes(schema: ObjectFullSchema): Option[JObject] = {
    schema.schema \ "properties" \ "attributes" match {
      case JObject(s) ⇒ Some(s)
      case _          ⇒ None
    }
  }

  def validateObjectBySchema(schema: ObjectFullSchema, form: ObjectForm, shadow: ObjectShadow)(
      implicit ec: EC): DbResultT[Json] = {
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
        case head :: tail ⇒ DbResultT.failures[Json](NonEmptyList(head, tail))
        case Nil          ⇒ DbResultT.good(illuminated)
      }
    }
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

  def projectFlatAttributes(formJson: Json, shadowJson: Json): Json =
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

  def validateAttributes(formJson: Json, shadowJson: Json): Seq[Failure] =
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
  def validateAttributesTypes(formJson: Json, shadowJson: Json): Seq[Failure] = {
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
  }

  private def validateAttributeType(attr: String,
                                    key: String,
                                    typed: JValue,
                                    form: Json): Seq[Failure] = {
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

  private def validateAttribute(attr: String,
                                key: String,
                                typed: JValue,
                                form: Json): Seq[Failure] = {
    val value = form \ key

    val shadowAttributesErrors = value match {
      case JNothing ⇒ Seq(ShadowHasInvalidAttribute(attr, key))
      case _        ⇒ Seq.empty[Failure]
    }

    validateAttributeType(attr, key, typed, form) ++ shadowAttributesErrors
  }

}
