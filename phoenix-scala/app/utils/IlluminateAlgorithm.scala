package utils

import failures.ObjectFailures._
import failures._
import models.objects._
import org.json4s.JsonAST.{JNothing, JObject, JString}
import org.json4s.JsonDSL._
import utils.aliases._
import utils.db._

// json schema
import scala.collection.JavaConverters._
import com.networknt.schema.JsonSchemaFactory
import com.fasterxml.jackson.databind.JsonNode
import org.json4s.jackson.JsonMethods.asJsonNode

object IlluminateAlgorithm {
  implicit val formats  = JsonFormatters.phoenixFormats
  val jsonSchemaFactory = new JsonSchemaFactory

  def get(attr: String, form: Json, shadow: Json): Json = shadow \ attr \ "ref" match {
    case JString(key) ⇒ form \ key
    case _            ⇒ JNothing
  }

  def validateObjectBySchema(schema: ObjectFullSchema, form: ObjectForm, shadow: ObjectShadow)(
      implicit ec: EC): DbResultT[Json] = {
    val illuminated          = projectAttributes(form.attributes, shadow.attributes)
    val jsonSchema: JsonNode = asJsonNode(schema.schema)

    val validator = jsonSchemaFactory.getSchema(jsonSchema)

    val errorMessages = validator.validate(asJsonNode(illuminated)).asScala
    if (errorMessages.isEmpty)
      DbResultT.good(illuminated)
    else
      DbResultT.failure[Json](
          ObjectValidationFailure(form.kind, shadow.id, errorMessages.mkString("\n")))
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
