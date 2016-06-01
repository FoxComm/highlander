package services.jobs

import scala.concurrent.duration._

import slick.driver.PostgresDriver.api._
import utils.aliases._

case class ProductsCatalogJob(implicit db: DB) extends SimpleJob {
  val initialDelay = 1.minute
  val interval     = 1.minute

  def job(): Unit = {
    db.run(sqlu"select public.toggle_products_catalog_from_to_active()")
  }
}
