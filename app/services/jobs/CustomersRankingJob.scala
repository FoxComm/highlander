package services.jobs

import scala.concurrent.duration._

import slick.driver.PostgresDriver.api._
import utils.aliases._

case class CustomersRankingJob(implicit db: DB) extends SimpleJob {
  val initialDelay = 1.minute
  val interval     = 5.minutes

  def job(): Unit = {
    db.run(sqlu"select public.update_customers_ranking()")
  }
}
