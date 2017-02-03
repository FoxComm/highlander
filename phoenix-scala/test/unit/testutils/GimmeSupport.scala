package testutils

import scala.language.implicitConversions
import cats.data.{Xor, XorT}
import failures.Failures
import org.scalatest.{AppendedClues, EitherValues}
import org.scalatest.concurrent.ScalaFutures
import scala.concurrent.Future
import scala.reflect.runtime.universe.TypeTag
import services.Result
import slick.driver.PostgresDriver.api._
import slick.lifted.Query
import utils.aliases._

trait GimmeSupport extends ScalaFutures with EitherValues with CatsHelpers with AppendedClues {

  implicit class GimmeQuery[E, U, C[_]](val query: Query[E, U, C]) {
    // allows us to do Table.someQuery.gimme vs Table.someQuery.result.run().futureValue
    def gimme(implicit ec: EC, db: DB, line: SL, file: SF): C[U] =
      db.run(query.result).futureValue withClue originalSourceClue

    def gimmeTxn(implicit ec: EC, db: DB, line: SL, file: SF): C[U] =
      db.run(query.result.transactionally).futureValue withClue originalSourceClue
  }

  implicit class GimmeRep[R](val rep: Rep[R]) {

    def gimme(implicit ec: EC, db: DB, line: SL, file: SF): R =
      db.run(rep.result).futureValue withClue originalSourceClue

    def gimmeTxn(implicit ec: EC, db: DB, line: SL, file: SF): R =
      db.run(rep.result.transactionally).futureValue withClue originalSourceClue
  }

  implicit class GimmeDBIO[R](val dbio: DBIO[R]) {

    def gimme(implicit ec: EC, db: DB, line: SL, file: SF): R =
      db.run(dbio).futureValue withClue originalSourceClue

    def gimmeTxn(implicit ec: EC, db: DB, line: SL, file: SF): R =
      db.run(dbio.transactionally).futureValue withClue originalSourceClue
  }

  implicit class GimmeDbResult[R](val dbResult: DBIO[Failures Xor R]) {

    def gimme(implicit tt: TypeTag[R], ec: EC, db: DB, line: SL, file: SF): R =
      db.run(dbResult).futureValue.rightVal withClue originalSourceClue

    def gimmeTxn(implicit tt: TypeTag[R], ec: EC, db: DB, line: SL, file: SF): R =
      db.run(dbResult.transactionally).futureValue.rightVal withClue originalSourceClue
  }

  implicit class GimmeXorT[R](val xorT: XorT[DBIO, Failures, R]) {

    def gimme(implicit tt: TypeTag[R], ec: EC, db: DB, line: SL, file: SF): R =
      db.run(xorT.value).futureValue.rightVal withClue originalSourceClue

    def gimmeTxn(implicit tt: TypeTag[R], ec: EC, db: DB, line: SL, file: SF): R =
      db.run(xorT.value.transactionally).futureValue.rightVal withClue originalSourceClue
  }

  implicit class GimmeResult[R](val res: Result[R]) {

    def gimme(implicit tt: TypeTag[R], ec: EC, line: SL, file: SF): R =
      res.futureValue.rightVal withClue originalSourceClue
  }

  implicit class GimmeFutureResult[L, R](val res: Future[Either[L, R]]) {

    def gimme(implicit tt: TypeTag[R], ec: EC, line: SL, file: SF): R =
      res.futureValue.right.value withClue originalSourceClue
  }
}
