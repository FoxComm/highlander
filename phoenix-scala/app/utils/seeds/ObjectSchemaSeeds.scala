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
      price   ← * <~ ObjectSchemas.create(priceSchema)
      sku     ← * <~ ObjectSchemas.create(skuSchema)
      product ← * <~ ObjectSchemas.create(productSchema)
    } yield product

  private def loadJson(fileName: String): JValue = {
    val stream = getClass.getResourceAsStream(fileName)
    parse(scala.io.Source.fromInputStream(stream).mkString)
  }

  def productSchema: ObjectSchema = ObjectSchema(
      name = "product",
      dependencies = List("sku"),
      schema = loadJson("/object_schemas/product.json")
  )

  def skuSchema: ObjectSchema = ObjectSchema(
      name = "sku",
      dependencies = List("price"),
      schema = loadJson("/object_schemas/sku.json")
  )

  def priceSchema: ObjectSchema = ObjectSchema(
      name = "price",
      dependencies = List.empty[String],
      schema = loadJson("/object_schemas/price.json")
  )
}
