package utils

import failures.ObjectFailures._
import failures._
import models.objects._
import org.json4s.JsonAST.{JNothing, JObject, JString}
import org.json4s.JsonDSL._
import utils.aliases._
import utils.db._

// json schema
import com.fasterxml.jackson.databind.JsonNode
import com.github.fge.jsonschema.main.JsonSchemaFactory
import org.json4s.jackson.JsonMethods.asJsonNode

object IlluminateAlgorithm {
  implicit val formats = JsonFormatters.phoenixFormats

  def get(attr: String, form: Json, shadow: Json): Json = shadow \ attr \ "ref" match {
    case JString(key) ⇒ form \ key
    case _            ⇒ JNothing
  }

  def validateObjectBySchema(illuminated: Json, jsonSchema: Json)(
      implicit ec: EC): DbResultT[Json] = {
    val schema: JsonNode   = asJsonNode(jsonSchema)
    val instance: JsonNode = asJsonNode(illuminated)

    val validator = JsonSchemaFactory.byDefault().getValidator

    val processingReport = validator.validate(schema, instance)

    if (processingReport.isSuccess)
      DbResultT.good(illuminated)
    else
      DbResultT.failure[Json](ObjectValidationFailure(processingReport.toString))
  }

  def illuminateAttributes(schema: Json, formJson: Json, shadowJson: Json)(
      implicit ec: EC): DbResultT[Json] = {
    val attributes = projectAttributes(formJson, shadowJson)
    validateObjectBySchema(attributes, schema)
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
