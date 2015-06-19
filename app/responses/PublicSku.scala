package responses

import models._

import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import scala.concurrent.{ExecutionContext, Future}

object PublicSku {
  type Response = Future[Option[Root]]

  case class Root(id: Int, name: String, availableForSale: Boolean)

  def build(sku: Sku, availableForSale: Boolean): Root = {
    Root(id = sku.id, name = sku.name.getOrElse(""), availableForSale = availableForSale)
  }

  def findById(id: Int)
              (implicit ec: ExecutionContext, db: Database): Response = {

    val queries = for {
      sku <- Skus._findById(id)
      availableForSale = Skus._isAvailableOnHand(id)
    } yield (sku, availableForSale)

    db.run(queries.take(1).result.headOption).map { result =>
      result.map { case (sku, available) => build(sku, available) }
    }
  }

}
