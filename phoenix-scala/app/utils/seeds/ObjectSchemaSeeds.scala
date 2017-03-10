package utils.seeds

import scala.concurrent.ExecutionContext.Implicits.global

import models.objects._
import org.json4s.JValue
import org.json4s.jackson.JsonMethods._
import utils.db._

trait ObjectSchemaSeeds {

  // Some tests have invalid data ⇒ can't create all schemas
  // Will be fixed in scope of promotion work
  def createObjectSchemasForTest(contextId: Int): DbResultT[Unit] =
    for {
      _ ← * <~ ObjectSchemas.create(getSchema("empty", contextId))
      _ ← * <~ ObjectSchemas.create(getSchema("album", contextId))
      _ ← * <~ ObjectSchemas.create(getSchema("image", contextId))
      _ ← * <~ ObjectSchemas.create(getSchema("price", contextId))
      _ ← * <~ ObjectSchemas.create(getSchema("product-variant", contextId))
      _ ← * <~ ObjectSchemas.create(getSchema("coupon", contextId))
      _ ← * <~ ObjectSchemas.create(getSchema("discount", contextId))
      // _ ← * <~ ObjectSchemas.create(getSchema("promotion"))
      _ ← * <~ ObjectSchemas.create(getSchema("product", contextId))
    } yield {}

  def createObjectSchemas(contextId: Int): DbResultT[Unit] =
    for {
      _ ← * <~ ObjectSchemas.create(getSchema("empty", contextId))
      _ ← * <~ ObjectSchemas.create(getSchema("album", contextId))
      _ ← * <~ ObjectSchemas.create(getSchema("image", contextId))
      _ ← * <~ ObjectSchemas.create(getSchema("price", contextId))
      _ ← * <~ ObjectSchemas.create(getSchema("product-variant", contextId))
      _ ← * <~ ObjectSchemas.create(getSchema("coupon", contextId))
      _ ← * <~ ObjectSchemas.create(getSchema("discount", contextId))
      _ ← * <~ ObjectSchemas.create(getSchema("promotion", contextId))
      _ ← * <~ ObjectSchemas.create(getSchema("product", contextId))
    } yield {}

  private def loadJson(fileName: String): JValue = {
    val streamMaybe = Option(getClass.getResourceAsStream(fileName))
    streamMaybe.fold {
      throw new java.io.FileNotFoundException(s"schema $fileName not found")
    } { stream ⇒
      parse(scala.io.Source.fromInputStream(stream).mkString)
    }
  }

  def getSchema(name: String, contextId: Int): ObjectSchema = {
    val schema       = loadJson(s"/object_schemas/$name.json")
    val dependencies = getDependencies(schema).toList
    ObjectSchema(contextId = contextId,
                 kind = name,
                 name = name,
                 dependencies = dependencies,
                 schema = schema)
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
