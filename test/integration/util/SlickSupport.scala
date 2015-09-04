package util

import scala.concurrent.{ExecutionContext, Future}
import org.scalatest.concurrent.AbstractPatienceConfiguration
import slick.driver.PostgresDriver.api._
import slick.lifted.Query

object SlickSupport {
  object implicits {
    implicit class EnrichedQuery[E, U, C[_]](val query: Query[E, U, C])
      extends AnyVal {

      // allows us to do Model.someQuery.futureValue vs Model.someQuery.result.run().futureValue
      def futureValue(implicit db: Database, ec: ExecutionContext): C[U] = {
        import org.scalatest.concurrent.ScalaFutures._
        db.run(query.result).futureValue
      }
    }

    implicit class EnrichedDBIO[R](val dbio: DBIO[R]) extends AnyVal {
      def futureValue(implicit db: Database, ec: ExecutionContext): R = {
        import org.scalatest.concurrent.ScalaFutures._
        db.run(dbio).futureValue
      }
    }
  }
}
