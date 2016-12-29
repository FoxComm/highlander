package utils.seeds

import java.io.FileNotFoundException

import scala.concurrent.ExecutionContext.Implicits.global

import cats.implicits._
import models.objects._
import org.json4s.JValue
import utils.db._
import org.json4s.jackson.JsonMethods._

trait ObjectSchemaSeeds {

  def createObjectSchemas(): DbResultT[ObjectSchema] =
    for {
      empty     ← * <~ ObjectSchemas.create(getSchema("empty"))
      _         ← * <~ ObjectSchemas.create(getSchema("album"))
      _         ← * <~ ObjectSchemas.create(getSchema("image"))
      price     ← * <~ ObjectSchemas.create(getSchema("price"))
      _         ← * <~ ObjectSchemas.create(getSchema("product-variant"))
      coupon    ← * <~ ObjectSchemas.create(getSchema("coupon"))
      discount  ← * <~ ObjectSchemas.create(getSchema("discount"))
      promotion ← * <~ ObjectSchemas.create(getSchema("promotion"))
      product   ← * <~ ObjectSchemas.create(getSchema("product"))
    } yield product

  private def loadJson(fileName: String): JValue = {
    val streamMaybe = Option(getClass.getResourceAsStream(fileName))
    streamMaybe.fold {
      throw new java.io.FileNotFoundException(s"schema $fileName not found")
    } { stream ⇒
      parse(scala.io.Source.fromInputStream(stream).mkString)
    }
  }

  def getSchema(name: String): ObjectSchema = {
    val schema       = loadJson(s"/object_schemas/$name.json")
    val dependencies = getDependencies(schema).toList
    ObjectSchema(kind = name, name = name, dependencies = dependencies, schema = schema)
  }

  private def getDependencies(schema: JValue): Set[String] = {
    implicit val formats = utils.JsonFormatters.phoenixFormats

    val depValue = (s: String) ⇒ s.drop("#/definitions/".length)
    schema.foldField(Set.empty[String]) {
      case (acc, (key, value)) ⇒
        key match {
          case "$ref" ⇒ acc ++ Set(depValue(value.extract[String]))
          case _      ⇒ acc
        }
    }
  }

}
