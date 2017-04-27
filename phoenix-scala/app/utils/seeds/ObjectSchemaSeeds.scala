package utils.seeds

import models.objects._
import scala.concurrent.ExecutionContext.Implicits.global
import utils.aliases._
import utils.db._
import utils.json.yolo._

trait ObjectSchemaSeeds {

  private val allButPromoSchemaNames = Seq("empty",
                                           "album",
                                           "image",
                                           "price",
                                           "sku",
                                           "coupon",
                                           "discount",
                                           "product",
                                           "taxonomy",
                                           "taxon")
  private val allSchemaNames = allButPromoSchemaNames :+ "promotion"

  private lazy val allButPromoSchemas = allButPromoSchemaNames.map(getSchema)
  private lazy val allSchemas         = allSchemaNames.map(getSchema)

  // Some tests have invalid data ⇒ can't create all schemas
  // TODO @anna @michalrus fix this in scope of promotion work
  def FIXME_createAllButPromoSchemas(): DbResultT[Option[Int]] =
    ObjectSchemas.createAll(allButPromoSchemas)

  def createObjectSchemas(): DbResultT[Option[Int]] =
    ObjectSchemas.createAll(allSchemas)

  private def loadJson(fileName: String): Json = {
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

  private def getDependencies(schema: Json): Set[String] = {
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
