package objectframework.payloads

import cats.data.NonEmptyList
import com.networknt.schema.JsonSchemaFactory
import core.db._
import objectframework.ObjectFailures._
import objectframework.models._
import org.json4s.JsonAST._
import org.json4s.jackson.JsonMethods._
import org.json4s.{Extraction, Formats}

import scala.collection.JavaConverters._

object ObjectSchemaValidation {

  trait SchemaValidation[M] { this: M ⇒
    def defaultSchemaName: String
    val schema: Option[String] = None

    private def validatePayload(payload: M, jsonSchema: JValue)(implicit ec: EC, fmt: Formats): DbResultT[M] = {
      val jsonSchemaFactory = new JsonSchemaFactory
      val validator         = jsonSchemaFactory.getSchema(asJsonNode(jsonSchema))

      val jsonPayload = Extraction.decompose(payload)

      val flatPayload = jsonPayload.mapField {
        case (name, JObject(List(("t", _), ("v", v)))) ⇒
          JField(name, v)
        case x ⇒ x
      }

      val errorMessages = validator.validate(asJsonNode(flatPayload)).asScala.toList

      errorMessages.map { error ⇒
        PayloadValidationFailure(error.getMessage)
      } match {
        case head :: tail ⇒ DbResultT.failures[M](NonEmptyList(head, tail))
        case Nil          ⇒ DbResultT.good(payload)
      }

    }

    def validate(implicit ec: EC, fmt: Formats): DbResultT[M] = {
      val schemaName = schema.fold(defaultSchemaName)(identity)
      for {
        schema    ← * <~ ObjectFullSchemas.mustFindByName404(schemaName)
        validated ← * <~ validatePayload(this, schema.schema)
      } yield validated
    }

  }

}
