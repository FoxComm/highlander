package testutils

import scala.language.implicitConversions
import scala.reflect.runtime.universe.TypeTag
import cats.data._
import cats.implicits._
import failures.Failures
import org.scalatest.AppendedClues
import org.scalatest.concurrent.ScalaFutures
import services.Result
import slick.driver.PostgresDriver.api._
import slick.lifted.Query
import utils.aliases._
import utils.db._

trait GimmeSupport extends ScalaFutures with CatsHelpers with AppendedClues {

  // TODO: are the *Txn functions really used? @michalrus

  implicit class GimmeQuery[E, U, C[_]](query: Query[E, U, C]) {
    // allows us to do Table.someQuery.gimme vs Table.someQuery.result.run().futureValue
    def gimme(implicit ec: EC, db: DB, line: SL, file: SF): C[U] =
      db.run(query.result).futureValue withClue originalSourceClue

    def gimmeTxn(implicit ec: EC, db: DB, line: SL, file: SF): C[U] =
      db.run(query.result.transactionally).futureValue withClue originalSourceClue
  }

  implicit class GimmeRep[R](rep: Rep[R]) {

    def gimme(implicit ec: EC, db: DB, line: SL, file: SF): R =
      db.run(rep.result).futureValue withClue originalSourceClue

    def gimmeTxn(implicit ec: EC, db: DB, line: SL, file: SF): R =
      db.run(rep.result.transactionally).futureValue withClue originalSourceClue
  }

  implicit class GimmeDBIO[R](dbio: DBIO[R]) {

    def gimme(implicit ec: EC, db: DB, line: SL, file: SF): R =
      db.run(dbio).futureValue withClue originalSourceClue

    def gimmeTxn(implicit ec: EC, db: DB, line: SL, file: SF): R =
      db.run(dbio.transactionally).futureValue withClue originalSourceClue
  }

  implicit class GimmeDbResult[R](dbResult: DBIO[Failures Xor R]) { // TODO: is this used? @michalrus

    def gimme(implicit tt: TypeTag[R], ec: EC, db: DB, line: SL, file: SF): R =
      db.run(dbResult).futureValue.rightVal withClue originalSourceClue

    def gimmeTxn(implicit tt: TypeTag[R], ec: EC, db: DB, line: SL, file: SF): R =
      db.run(dbResult.transactionally).futureValue.rightVal withClue originalSourceClue
  }

  implicit class GimmeDbResultT[R](res: DbResultT[R]) {

    def gimme(implicit tt: TypeTag[R], ec: EC, db: DB, line: SL, file: SF): R =
      res.runDBIO().gimme

    def gimmeTxn(implicit tt: TypeTag[R], ec: EC, db: DB, line: SL, file: SF): R =
      res.runTxn().gimme

    def gimmeTxnFailures(implicit tt: TypeTag[R], ec: EC, db: DB, line: SL, file: SF): Failures =
      res.runTxn().gimmeFailures

    def gimmeFailures(implicit tt: TypeTag[R], ec: EC, db: DB, line: SL, file: SF): Failures =
      res.runDBIO().gimmeFailures

  }

  implicit class GimmeResult[R](res: Result[R]) {

    // TODO: maybe dump all warnings to info()? @michalrus

    def gimme(implicit tt: TypeTag[R], ec: EC, db: DB, line: SL, file: SF): R =
      res.runEmptyA.value.futureValue.rightVal withClue originalSourceClue

    def gimmeTxn(implicit tt: TypeTag[R], ec: EC, db: DB, line: SL, file: SF): R =
      res.gimme // TODO: why? @michalrus

    def gimmeFailures(implicit tt: TypeTag[R], ec: EC, db: DB, line: SL, file: SF): Failures =
      res.runEmptyA.value.futureValue.leftVal withClue originalSourceClue

  }
}
