package utils

import scala.concurrent.Future

import cats.data.{Xor, XorT}
import failures._
import slick.driver.PostgresDriver.api._
import slick.jdbc.SQLActionBuilder
import slick.lifted.Query
import slick.profile.{SqlAction, SqlStreamingAction}
import utils.aliases._
import utils.time.JavaTimeSlickMapper

package object db {

  type DbResult[T]  = DBIO[Failures Xor T]
  type DbResultT[A] = XorT[DBIO, Failures, A]
  // Return B whenever A is inserted
  type Returning[A, B] = slick.driver.JdbcActionComponent#ReturningInsertActionComposer[A, B]

  implicit val javaTimeSlickMapper      = JavaTimeSlickMapper.instantAndTimestampWithoutZone
  implicit val currencyColumnTypeMapper = Money.currencyColumnType

  abstract class FoxTable[M <: FoxModel[M]](tag: Tag, name: String) extends Table[M](tag, name) {
    def id: Rep[M#Id]
  }

  def appendForUpdate[A, B <: slick.dbio.NoStream](sql: SqlAction[A, B, Effect.Read]): DBIO[A] = {
    sql.overrideStatements(sql.statements.map(_ + " for update"))
  }

  def lift[A](value: A): DBIO[A] = DBIO.successful(value)

  def liftFuture[A](future: Future[A]): DBIO[A] = DBIO.from(future)

  implicit class EnrichedSQLActionBuilder(val action: SQLActionBuilder) extends AnyVal {
    def stripMargin: SQLActionBuilder =
      action.copy(action.queryParts.map(_.asInstanceOf[String].stripMargin))
  }

  implicit class EnrichedQuery[E, U, C[_]](val query: Query[E, U, C]) extends AnyVal {
    def one: DBIO[Option[U]] = query.result.headOption

    def mustFindOneOr(notFoundFailure: Failure)(implicit ec: EC): DbResult[U] =
      query.one.mustFindOr(notFoundFailure)

    def mustNotFindOneOr(notFoundFailure: Failure)(implicit ec: EC): DbResult[Unit] =
      query.one.mustNotFindOr(notFoundFailure)
  }

  implicit class EnrichedSqlStreamingAction[R, T, E <: Effect](
      val action: SqlStreamingAction[R, T, E])
      extends AnyVal {

    def one(implicit db: DB): Future[Option[T]] =
      db.run(action.headOption)
  }

  implicit class RunOnDbIO[R](val dbio: DBIO[R]) extends AnyVal {
    def run()(implicit db: Database): Future[R] =
      db.run(dbio)

    def toXor(implicit ec: EC): DbResult[R] =
      DbResult.fromDbio(dbio)
  }

  sealed trait FoundOrCreated
  case object Found   extends FoundOrCreated
  case object Created extends FoundOrCreated

  implicit class EnrichedDBIOpt[R](val dbio: DBIO[Option[R]]) extends AnyVal {

    def findOrCreate(r: DbResult[R])(implicit ec: EC): DbResult[R] = {
      dbio.flatMap {
        case Some(model) ⇒ DbResult.good(model)
        case None        ⇒ r
      }
    }

    // Last item in tuple determines if cart was created or not
    def findOrCreateExtended(r: DbResult[R])(implicit ec: EC): DbResult[(R, FoundOrCreated)] = {
      dbio.flatMap {
        case Some(model) ⇒ DbResult.good((model, Found))
        case _           ⇒ r.map(_.map(result ⇒ (result, Created)))
      }
    }

    def mustFindOr(notFoundFailure: Failure)(implicit ec: EC): DbResult[R] = dbio.flatMap {
      case Some(model) ⇒ DbResult.good(model)
      case None        ⇒ DbResult.failure(notFoundFailure)
    }

    def mustNotFindOr(shouldNotBeHere: Failure)(implicit ec: EC): DbResult[Unit] = dbio.flatMap {
      case None    ⇒ DbResult.unit
      case Some(_) ⇒ DbResult.failure(shouldNotBeHere)
    }

    // we only use this when we *know* we can call head safely on a query. (e.g., you've created a record which
    // has a FK constraint to another table and you then fetch that associated record -- we already *know* it must
    // exist.
    def safeGet(implicit ec: EC): DBIO[R] = dbio.map(_.get)
  }

  implicit class EnrichedDbResult[A](val r: DbResult[A]) extends AnyVal {
    def toXorT: DbResultT[A] = DbResultT(r)
  }

  def xorMapDbio[LeftX, RightX, RightY](xor: Xor[LeftX, RightX])(f: RightX ⇒ DBIO[RightY])(
      implicit ec: EC): DBIO[Xor[LeftX, RightY]] = {
    xor.fold(
        fs ⇒ lift(Xor.left(fs)),
        v ⇒ f(v).map(Xor.right)
    )
  }
}
