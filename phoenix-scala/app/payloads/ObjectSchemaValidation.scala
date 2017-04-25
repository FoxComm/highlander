package payloads

import cats.data.NonEmptyList
import com.networknt.schema.JsonSchemaFactory
import failures.ObjectFailures._
import io.circe._
import io.circe.syntax._
import models.objects._
import scala.collection.JavaConverters._
import utils.aliases._
import utils.db._
import utils.json._

object ObjectSchemaValidation {
  trait SchemaValidation[M] {
    this: M ⇒
    def defaultSchemaName: String
    val schema: Option[String] = None

    private def validatePayload(payload: M, jsonSchema: Json)(
        implicit ec: EC,
        encoder: Encoder[M]): DbResultT[M] = {
      val jsonSchemaFactory = new JsonSchemaFactory
      val validator         = jsonSchemaFactory.getSchema(jsonSchema.asJsonNode)

      val flatPayload = payload.asJson.transformField {
        case (name, json) ⇒
          name → json.withObject {
            case obj if obj.contains("t") ⇒ obj("v").getOrElse(Json.fromJsonObject(obj))
            case obj                      ⇒ Json.fromJsonObject(obj)
          }
      }

      val errorMessages = validator.validate(flatPayload.asJsonNode).asScala.toList

      errorMessages.map { error ⇒
        PayloadValidationFailure(error.getMessage)
      } match {
        case head :: tail ⇒ DbResultT.failures[M](NonEmptyList(head, tail))
        case Nil          ⇒ DbResultT.good(payload)
      }

    }

    def validate(implicit ec: EC): DbResultT[M] = {
      val schemaName = schema.fold(defaultSchemaName)(identity)
      for {
        schema    ← * <~ ObjectFullSchemas.mustFindByName404(schemaName)
        validated ← * <~ validatePayload(this, schema.schema)
      } yield validated
    }

  }

}
