package utils.seeds

import models.objects._
import org.json4s.JValue
import org.json4s.jackson.JsonMethods._
import slick.driver.PostgresDriver.api._
import utils.db._

import scala.concurrent.ExecutionContext.Implicits.global

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

  def upgradeObjectSchemas(): DbResultT[Unit] =
    for {
      ss ← * <~ ObjectSchemas.result
      _ ← * <~ ss.map(
             s ⇒
               ObjectSchemas.update(
                   s,
                   s.copy(schema =
                         allSchemas.find(sc ⇒ s.name == sc.name).headOption.getOrElse(s).schema)))
    } yield ()

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
