package utils.seeds

import scala.concurrent.ExecutionContext.Implicits.global

import models.objects._
import org.json4s.JValue
import org.json4s.jackson.JsonMethods._
import utils.db._

trait ObjectSchemaSeeds {

  // Some tests have invalid data ⇒ can't create all schemas
  // Will be fixed in scope of promotion work
  def createObjectSchemasForTest(): DbResultT[Unit] =
    for {
      _ ← * <~ ObjectSchemas.create(getSchema("empty"))
      _ ← * <~ ObjectSchemas.create(getSchema("album"))
      _ ← * <~ ObjectSchemas.create(getSchema("image"))
      _ ← * <~ ObjectSchemas.create(getSchema("price"))
      _ ← * <~ ObjectSchemas.create(getSchema("product-variant"))
      _ ← * <~ ObjectSchemas.create(getSchema("coupon"))
      _ ← * <~ ObjectSchemas.create(getSchema("discount"))
      // _ ← * <~ ObjectSchemas.create(getSchema("promotion"))
      _ ← * <~ ObjectSchemas.create(getSchema("product"))
    } yield {}

  def createObjectSchemas(): DbResultT[Unit] =
    for {
      _ ← * <~ ObjectSchemas.create(getSchema("empty"))
      _ ← * <~ ObjectSchemas.create(getSchema("album"))
      _ ← * <~ ObjectSchemas.create(getSchema("image"))
      _ ← * <~ ObjectSchemas.create(getSchema("price"))
      _ ← * <~ ObjectSchemas.create(getSchema("product-variant"))
      _ ← * <~ ObjectSchemas.create(getSchema("coupon"))
      _ ← * <~ ObjectSchemas.create(getSchema("discount"))
      _ ← * <~ ObjectSchemas.create(getSchema("promotion"))
      _ ← * <~ ObjectSchemas.create(getSchema("product"))
    } yield {}

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
