package responses

import scala.concurrent.{ExecutionContext, Future}

import models._
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._

object PublicSku {
  type Response = Future[Option[Root]]

  final case class Root(id: Int, name: String, availableForSale: Boolean)

  def build(sku: Sku, availableForSale: Boolean): Root = {
    Root(id = sku.id, name = sku.name.getOrElse(""), availableForSale = availableForSale)
  }

  def findById(id: Int)
              (implicit ec: ExecutionContext, db: Database): Response = {

    val queries = for {
      sku ← Skus.findById(id).extract
      availableForSale = Skus.isAvailableOnHand(id)
    } yield (sku, availableForSale)

    db.run(queries.one).map { result ⇒
      result.map { case (sku, available) ⇒ build(sku, available) }
    }
  }

}
