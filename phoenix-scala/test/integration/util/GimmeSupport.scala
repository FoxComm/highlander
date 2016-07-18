package util

import scala.language.implicitConversions
import scala.reflect.runtime.universe.TypeTag

import cats.data.{Xor, XorT}
import failures.Failures
import org.scalatest.concurrent.ScalaFutures
import services.Result
import slick.driver.PostgresDriver.api._
import slick.lifted.Query
import utils.aliases._

trait GimmeSupport extends ScalaFutures with CatsHelpers {

  implicit class GimmeQuery[E, U, C[_]](val query: Query[E, U, C]) {
    // allows us to do Table.someQuery.gimme vs Table.someQuery.result.run().futureValue
    def gimme(implicit ec: EC, db: DB): C[U] =
      db.run(query.result).futureValue
  }

  implicit class GimmeRep[R](val rep: Rep[R]) {
    def gimme(implicit ec: EC, db: DB): R =
      db.run(rep.result).futureValue
  }

  implicit class GimmeDBIO[R](val dbio: DBIO[R]) {
    def gimme(implicit ec: EC, db: DB): R =
      db.run(dbio).futureValue
  }

  implicit class GimmeDbResult[R](val dbResult: DBIO[Failures Xor R]) {
    def gimme(implicit tt: TypeTag[R], ec: EC, db: DB): R =
      db.run(dbResult).futureValue.rightVal
  }

  implicit class GimmeXorT[R](val xorT: XorT[DBIO, Failures, R]) {
    def gimme(implicit tt: TypeTag[R], ec: EC, db: DB): R =
      db.run(xorT.value).futureValue.rightVal
  }

  implicit class GimmeResult[R](val res: Result[R]) {
    def gimme(implicit tt: TypeTag[R], ec: EC, db: DB): R =
      res.futureValue.rightVal
  }
}
