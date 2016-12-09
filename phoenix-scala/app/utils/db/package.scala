package utils

import scala.concurrent.Future

import cats.data.{NonEmptyList, Xor, XorT}
import cats.implicits._
import cats.{Applicative, Functor, Monad}
import failures._
import services.Result
import slick.dbio.DBIOAction
import slick.driver.PostgresDriver.api._
import cats.FlatMap
import slick.jdbc.SQLActionBuilder
import slick.lifted.Query
import slick.profile.SqlAction
import utils.aliases._
import utils.time.JavaTimeSlickMapper

package object db {

  type DbResultT[A] = XorT[DBIO, Failures, A]
  // DBIO monad
  implicit def dbioApplicative(implicit ec: EC): Applicative[DBIO] = new Applicative[DBIO] {
    def ap[A, B](f: DBIO[A ⇒ B])(fa: DBIO[A]): DBIO[B] =
      fa.flatMap(a ⇒ f.map(ff ⇒ ff(a)))

    def pure[A](a: A): DBIO[A] = DBIO.successful(a)
  }

  implicit def dbioMonad(implicit ec: EC) = new Functor[DBIO] with Monad[DBIO] {
    override def map[A, B](fa: DBIO[A])(f: A ⇒ B): DBIO[B] = fa.map(f)

    override def pure[A](a: A): DBIO[A] = DBIO.successful(a)

    override def flatMap[A, B](fa: DBIO[A])(f: A ⇒ DBIO[B]): DBIO[B] = fa.flatMap(f)

    override def tailRecM[A, B](a: A)(f: A ⇒ DBIO[Either[A, B]]): DBIO[B] =
      defaultTailRecM(a)(f)
  }

  // implicits
  implicit class EnrichedDbResultT[A](dbResultT: DbResultT[A]) {
    def runTxn()(implicit ec: EC, db: DB): Result[A] =
      dbResultT
      // turn `left` into `DBIO.failed` to force transaction rollback
        .fold(failures ⇒ DBIO.failed(FoxFailureException(failures)), good ⇒ DBIO.successful(good))
        .flatMap(a ⇒ a) // flatten...
        .transactionally
        .dbresult // just a DBIO ⇒ DbResultT wrapper
        .run()    // throws a FoxFailureException :/
        .recover { // don't actually want an exception thrown, so wrap it back
          case e: FoxFailureException ⇒ Xor.left(e.failures)
        }

    def run()(implicit db: DB): Result[A] =
      dbResultT.value.run()

    def meh(implicit ec: EC): DbResultT[Unit] = for (_ ← * <~ dbResultT) yield {}
  }

  final implicit class EnrichedOption[A](val option: Option[A]) extends AnyVal {
    def toXor[F](or: F): F Xor A =
      option.fold { Xor.left[F, A](or) }(Xor.right[F, A])
  }

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

  def doOrMeh(condition: Boolean, action: DbResultT[_])(implicit ec: EC): DbResultT[Unit] =
    if (condition) action.meh else DbResultT.unit

  def doOrGood[A](condition: Boolean, action: DbResultT[A], good: A)(
      implicit ec: EC): DbResultT[A] =
    if (condition) action else DbResultT.good(good)

  def doOrFail[A](condition: Boolean, action: DbResultT[A], failure: Failure)(
      implicit ec: EC): DbResultT[A] =
    if (condition) action else DbResultT.failure[A](failure)

  def failIf(condition: Boolean, failure: Failure)(implicit ec: EC): DbResultT[Unit] =
    if (condition) DbResultT.failure[Unit](failure) else DbResultT.unit

  def failIfNot(condition: Boolean, failure: Failure)(implicit ec: EC): DbResultT[Unit] =
    failIf(!condition, failure)

  def failIfFailures(failures: Seq[Failure])(implicit ec: EC): DbResultT[Unit] =
    failures match {
      case head :: tail ⇒
        DbResultT.failures[Unit](NonEmptyList.of(head, tail: _*))
      case _ ⇒
        DbResultT.unit
    }

  implicit class EnrichedSQLActionBuilder(val action: SQLActionBuilder) extends AnyVal {
    def stripMargin: SQLActionBuilder =
      action.copy(action.queryParts.map(_.asInstanceOf[String].stripMargin))
  }

  implicit class EnrichedQuery[E, U, C[_]](val query: Query[E, U, C]) extends AnyVal {
    def one: DBIO[Option[U]] = query.result.headOption

    def mustFindOneOr(notFoundFailure: Failure)(implicit ec: EC): DbResultT[U] =
      query.one.mustFindOr(notFoundFailure)

    def mustNotFindOneOr(notFoundFailure: Failure)(implicit ec: EC): DbResultT[Unit] =
      query.one.mustNotFindOr(notFoundFailure)
  }

  implicit class RunOnDbIO[R](val dbio: DBIO[R]) extends AnyVal {
    def run()(implicit db: DB): Future[R] =
      db.run(dbio)

    def dbresult(implicit ec: EC): DbResultT[R] =
      DbResultT.fromDbio(dbio)
  }

  sealed trait FoundOrCreated
  case object Found   extends FoundOrCreated
  case object Created extends FoundOrCreated

  implicit class EnrichedDBIOpt[R](val dbio: DBIO[Option[R]]) extends AnyVal {

    def findOrCreate(r: DbResultT[R])(implicit ec: EC): DbResultT[R] = {
      dbio.dbresult.flatMap {
        case Some(model) ⇒ DbResultT.good(model)
        case None        ⇒ r
      }
    }

    // Last item in tuple determines if cart was created or not
    def findOrCreateExtended(r: DbResultT[R])(implicit ec: EC): DbResultT[(R, FoundOrCreated)] = {
      dbio.dbresult.flatMap {
        case Some(model) ⇒ DbResultT.good((model, Found))
        case _           ⇒ r.map(result ⇒ (result, Created))
      }
    }

    def mustFindOr(notFoundFailure: Failure)(implicit ec: EC): DbResultT[R] =
      dbio.dbresult.flatMap {
        case Some(model) ⇒ DbResultT.good(model)
        case None        ⇒ DbResultT.failure(notFoundFailure)
      }

    def mustNotFindOr(shouldNotBeHere: Failure)(implicit ec: EC): DbResultT[Unit] =
      dbio.dbresult.flatMap {
        case None    ⇒ DbResultT.unit
        case Some(_) ⇒ DbResultT.failure(shouldNotBeHere)
      }

    // we only use this when we *know* we can call head safely on a query. (e.g., you've created a record which
    // has a FK constraint to another table and you then fetch that associated record -- we already *know* it must
    // exist.
    def safeGet(implicit ec: EC): DBIO[R] = dbio.map(_.get)
  }
}
