package util

import slick.driver.PostgresDriver.api._
import slick.lifted.Query
import utils.aliases._

object SlickSupport {
  object implicits {
    implicit class EnrichedQuery[E, U, C[_]](val query: Query[E, U, C])
      extends AnyVal {

      // allows us to do Model.someQuery.futureValue vs Model.someQuery.result.run().futureValue
      def futureValue(implicit ec: EC, db: DB): C[U] = {
        import org.scalatest.concurrent.ScalaFutures._
        db.run(query.result).futureValue
      }
    }

    implicit class EnrichedDBIO[R](val dbio: DBIO[R]) extends AnyVal {
      def futureValue(implicit ec: EC, db: DB): R = {
        import org.scalatest.concurrent.ScalaFutures._
        db.run(dbio).futureValue
      }
    }
  }
}
