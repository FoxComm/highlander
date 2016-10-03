package utils

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

import com.fasterxml.jackson.databind.JsonNode
import com.networknt.schema.JsonSchemaFactory
import org.json4s.jackson.JsonMethods.asJsonNode

object IlluminateAlgorithm extends LazyLogging {
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
    val illuminated = projectAttributes(form.attributes, shadow.attributes)
    getInternalAttributes(schema).fold {
      logger.warn(s"Can't find attributes in schema ${schema.name}")
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
      case _        ⇒ Seq.empty[Failure]
    }

}
