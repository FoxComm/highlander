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
      qtyAvailable <- Skus._qtyAvailableOnHand(id)
    } yield (sku, (qtyAvailable > 0) )

    db.run(queries.result.head).map { skuAndQty =>
      Some(build(skuAndQty._1, skuAndQty._2))
    }
  }

}
