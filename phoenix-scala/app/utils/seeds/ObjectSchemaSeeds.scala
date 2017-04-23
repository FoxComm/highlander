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

  def upgradeObjectSchemas(schemasToUpdate: Option[Seq[String]]): DbResultT[Unit] = {
    val toUpgrade = schemasToUpdate.fold(allSchemas) { listOfSchemas ⇒
      listOfSchemas.map(getSchema)
    }
    for {
      allCurrent ← * <~ ObjectSchemas.result
      _ ← * <~ toUpgrade.map { newSchema ⇒
           allCurrent.find(_.name == newSchema.name) match {
             case Some(current) ⇒
               Console.err.println(s"Found ${current.name}, update")
               ObjectSchemas
                 .update(current,
                         current.copy(schema = newSchema.schema,
                                      dependencies = newSchema.dependencies))
                 .meh
             case _ ⇒
               Console.err.println(s"Not found ${newSchema.name}, create")
               ObjectSchemas.create(newSchema).meh
           }
         }
    } yield ()
  }

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
