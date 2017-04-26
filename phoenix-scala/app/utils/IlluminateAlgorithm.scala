package utils

import cats.data._
import cats.implicits._
import com.networknt.schema.JsonSchemaFactory
import com.typesafe.scalalogging.LazyLogging
import failures.Failure
import failures.ObjectFailures._
import io.circe._
import java.time.Instant
import models.objects._
import scala.collection.JavaConverters._
import utils.aliases._
import utils.db._
import utils.json._

// FIXME @kjanosz: way too much duplication
object IlluminateAlgorithm extends LazyLogging {
  def get(attr: String, form: Json, shadow: Json): Option[Json] =
    shadow.hcursor
      .downField(attr)
      .downField("ref")
      .as[String]
      .toOption
      .flatMap(form.hcursor.downField(_).focus)

  private def getInternalAttributes(schema: ObjectFullSchema): Option[JsonObject] =
    schema.schema.hcursor.downField("properties").downField("attributes").focus.flatMap(_.asObject)

  def validateObjectBySchema(schema: ObjectFullSchema, form: ObjectForm, shadow: ObjectShadow)(
      implicit ec: EC): DbResultT[Json] = {
    val illuminated = projectFlatAttributes(form.attributes, shadow.attributes)
    getInternalAttributes(schema).fold {
      DbResultT.good(illuminated)
    } { jsonSchema ⇒
      val jsonSchemaFactory = new JsonSchemaFactory
      val validator         = jsonSchemaFactory.getSchema(Json.fromJsonObject(jsonSchema).asJsonNode)

      val errorMessages = validator.validate(illuminated.asJsonNode).asScala.toList

      errorMessages.map { err ⇒
        ObjectValidationFailure(form.kind, shadow.id, err.getMessage)
      } match {
        case head :: tail ⇒ DbResultT.failures[Json](NonEmptyList(head, tail))
        case Nil          ⇒ DbResultT.good(illuminated)
      }
    }
  }

  def projectAttributes(formJson: Json, shadowJson: Json): Json =
    formJson.asObject
      .map2(shadowJson.asObject)((_, _))
      .flatMap {
        case (_, shadow) ⇒
          shadow.toVector.map {
            case (attr, link) ⇒
              val linkC = link.hcursor

              for {
                ref   ← linkC.downField("ref").focus
                key   ← ref.asString
                tpe   ← linkC.downField("type").focus
                value ← formJson \ key
              } yield (attr, Json.obj("t" → tpe, "v" → value))
          }.sequenceU.map(Json.obj(_: _*))
      }
      .getOrElse(Json.Null)

  def projectFlatAttributes(formJson: Json, shadowJson: Json): Json =
    formJson.asObject
      .map2(shadowJson.asObject)((_, _))
      .flatMap {
        case (_, shadow) ⇒
          shadow.toVector.map {
            case (attr, link) ⇒
              for {
                ref   ← link.hcursor.downField("ref").focus
                key   ← ref.asString
                value ← formJson \ key
              } yield (attr, value)
          }.sequenceU.map(Json.obj(_: _*))
      }
      .getOrElse(Json.Null)

  def validateAttributes(formJson: Json, shadowJson: Json): Seq[Failure] =
    (formJson.asObject, shadowJson.asObject) match {
      case (Some(_), Some(shadow)) ⇒
        shadow.toVector.flatMap {
          case (attr, link) ⇒
            val linkC = link.hcursor

            val failures = for {
              ref ← linkC.downField("ref").focus
              key ← ref.asString
              tpe ← linkC.downField("type").focus
            } yield validateAttribute(attr, key, tpe, formJson)
            failures.getOrElse(Seq(ShadowAttributeMissingRef(attr)))
        }
      case (Some(_), None) ⇒ Seq(ShadowAttributesAreEmpty)
      case (None, Some(_)) ⇒ Seq(FormAttributesAreEmpty)
      case _               ⇒ Seq(FormAttributesAreEmpty, ShadowAttributesAreEmpty)
    }

  def validateAttributesTypes(formJson: Json, shadowJson: Json): Seq[Failure] =
    formJson.asObject
      .map2(shadowJson.asObject)((_, _))
      .map {
        case (_, shadow) ⇒
          shadow.toVector.flatMap {
            case (attr, link) ⇒
              val linkC = link.hcursor

              (for {
                ref ← linkC.downField("ref").focus
                key ← ref.asString
                tpe ← linkC.downField("type").focus
              } yield validateAttributeType(attr, key, tpe, formJson)).getOrElse(Seq.empty)
          }
      }
      .getOrElse(Seq.empty)

  private def validateAttributeType(attr: String,
                                    key: String,
                                    typed: Json,
                                    form: Json): Seq[Failure] = {
    val value = form \ key

    typed.asString match {
      case Some("datetime") ⇒
        if (value.flatMap(_.as[Instant].toOption).isEmpty)
          Seq(ShadowAttributeInvalidTime(attr, value.toString))
        else
          Seq.empty[Failure]
      case _ ⇒ Seq.empty[Failure]
    }
  }

  private def validateAttribute(attr: String, key: String, typed: Json, form: Json): Seq[Failure] = {
    val value = form \ key

    val shadowAttributesErrors = value match {
      case None ⇒ Seq(ShadowHasInvalidAttribute(attr, key))
      case _    ⇒ Seq.empty[Failure]
    }

    validateAttributeType(attr, key, typed, form) ++ shadowAttributesErrors
  }

}
