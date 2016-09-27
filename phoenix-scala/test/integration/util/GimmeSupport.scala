package util

import scala.language.implicitConversions
import scala.reflect.runtime.universe.TypeTag

import cats.data.{Xor, XorT}
import failures.Failures
import org.scalatest.AppendedClues
import org.scalatest.concurrent.ScalaFutures
import services.Result
import slick.driver.PostgresDriver.api._
import slick.lifted.Query
import utils.aliases._

trait GimmeSupport extends ScalaFutures with CatsHelpers with AppendedClues {

  def clue(implicit line: SL, file: SF) =
    s"""(Original source: ${file.value.split("/").last}:${line.value})"""

  implicit class GimmeQuery[E, U, C[_]](val query: Query[E, U, C]) {
    // allows us to do Table.someQuery.gimme vs Table.someQuery.result.run().futureValue
    def gimme(implicit ec: EC, db: DB, line: SL, file: SF): C[U] =
      db.run(query.result).futureValue withClue clue

    def gimmeTxn(implicit ec: EC, db: DB, line: SL, file: SF): C[U] =
      db.run(query.result.transactionally).futureValue withClue clue
  }

  implicit class GimmeRep[R](val rep: Rep[R]) {

    def gimme(implicit ec: EC, db: DB, line: SL, file: SF): R =
      db.run(rep.result).futureValue withClue clue

    def gimmeTxn(implicit ec: EC, db: DB, line: SL, file: SF): R =
      db.run(rep.result.transactionally).futureValue withClue clue
  }

  implicit class GimmeDBIO[R](val dbio: DBIO[R]) {

    def gimme(implicit ec: EC, db: DB, line: SL, file: SF): R =
      db.run(dbio).futureValue withClue clue

    def gimmeTxn(implicit ec: EC, db: DB, line: SL, file: SF): R =
      db.run(dbio.transactionally).futureValue withClue clue
  }

  implicit class GimmeDbResult[R](val dbResult: DBIO[Failures Xor R]) {

    def gimme(implicit tt: TypeTag[R], ec: EC, db: DB, line: SL, file: SF): R =
      db.run(dbResult).futureValue.rightVal withClue clue

    def gimmeTxn(implicit tt: TypeTag[R], ec: EC, db: DB, line: SL, file: SF): R =
      db.run(dbResult.transactionally).futureValue.rightVal withClue clue
  }

  implicit class GimmeXorT[R](val xorT: XorT[DBIO, Failures, R]) {

    def gimme(implicit tt: TypeTag[R], ec: EC, db: DB, line: SL, file: SF): R =
      db.run(xorT.value).futureValue.rightVal withClue clue

    def gimmeTxn(implicit tt: TypeTag[R], ec: EC, db: DB, line: SL, file: SF): R =
      db.run(xorT.value.transactionally).futureValue.rightVal withClue clue
  }

  implicit class GimmeResult[R](val res: Result[R]) {

    def gimme(implicit tt: TypeTag[R], ec: EC, db: DB, line: SL, file: SF): R =
      res.futureValue.rightVal withClue clue

    def gimmeTxn(implicit tt: TypeTag[R], ec: EC, db: DB, line: SL, file: SF): R =
      res.futureValue.rightVal withClue clue
  }
}
