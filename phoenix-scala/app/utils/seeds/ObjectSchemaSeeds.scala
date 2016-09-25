package utils.seeds

import scala.concurrent.ExecutionContext.Implicits.global

import cats.implicits._
import models.objects._
import org.json4s.JValue
import utils.db._
import org.json4s.jackson.JsonMethods._

trait ObjectSchemaSeeds {

  def createObjectSchemas: DbResultT[ObjectSchema] =
    for {
      price     ← * <~ ObjectSchemas.create(getSchema("price"))
      sku       ← * <~ ObjectSchemas.create(getSchema("sku", List("price")))
      coupon    ← * <~ ObjectSchemas.create(getSchema("coupon"))
      discount  ← * <~ ObjectSchemas.create(getSchema("discount"))
      promotion ← * <~ ObjectSchemas.create(getSchema("promotion", List("discount")))
      product   ← * <~ ObjectSchemas.create(getSchema("product", List("sku")))
    } yield product

  private def loadJson(fileName: String): JValue = {
    val stream = getClass.getResourceAsStream(fileName)
    parse(scala.io.Source.fromInputStream(stream).mkString)
  }

  def getSchema(name: String, dependencies: List[String] = List.empty[String]): ObjectSchema = {
    ObjectSchema(name = name,
                 dependencies = dependencies,
                 schema = loadJson(s"/object_schemas/$name.json"))
  }

}
