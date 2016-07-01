package utils

import scala.collection.generic.CanBuildFrom
import scala.concurrent.Future

import cats.data.{Validated, Xor, XorT}
import cats.{Applicative, Functor, Monad}
import failures._
import services.Result
import slick.driver.PostgresDriver.api._
import slick.jdbc.SQLActionBuilder
import slick.lifted.Query
import slick.profile.{SqlAction, SqlStreamingAction}
import utils.aliases._
import utils.time.JavaTimeSlickMapper

package object db {

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

    def mustFindOneOr(notFoundFailure: Failure)(implicit ec: EC): DbResultT[U] =
      query.one.mustFindOr(notFoundFailure)

    def mustNotFindOneOr(notFoundFailure: Failure)(implicit ec: EC): DbResultT[Unit] =
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

    def toXor(implicit ec: EC): DbResultT[R] =
      DbResultT.fromDbio(dbio)
  }

  sealed trait FoundOrCreated
  case object Found   extends FoundOrCreated
  case object Created extends FoundOrCreated

  implicit class EnrichedDBIOpt[R](val dbio: DBIO[Option[R]]) extends AnyVal {

    def findOrCreate(r: DbResultT[R])(implicit ec: EC): DbResultT[R] = {
      dbio.toXor.flatMap {
        case Some(model) ⇒ DbResultT.good(model)
        case None        ⇒ r
      }
    }

    // Last item in tuple determines if cart was created or not
    def findOrCreateExtended(r: DbResultT[R])(implicit ec: EC): DbResultT[(R, FoundOrCreated)] = {
      dbio.toXor.flatMap {
        case Some(model) ⇒ DbResultT.good((model, Found))
        case _           ⇒ r.map(result ⇒ (result, Created))
      }
    }

    def mustFindOr(notFoundFailure: Failure)(implicit ec: EC): DbResultT[R] = dbio.toXor.flatMap {
      case Some(model) ⇒ DbResultT.good(model)
      case None        ⇒ DbResultT.failure(notFoundFailure)
    }

    def mustNotFindOr(shouldNotBeHere: Failure)(implicit ec: EC): DbResultT[Unit] =
      dbio.toXor.flatMap {
        case None    ⇒ DbResultT.unit
        case Some(_) ⇒ DbResultT.failure(shouldNotBeHere)
      }

    // we only use this when we *know* we can call head safely on a query. (e.g., you've created a record which
    // has a FK constraint to another table and you then fetch that associated record -- we already *know* it must
    // exist.
    def safeGet(implicit ec: EC): DBIO[R] = dbio.map(_.get)
  }

  // DBIO monad
  implicit def dbioApplicative(implicit ec: EC): Applicative[DBIO] = new Applicative[DBIO] {
    def ap[A, B](fa: DBIO[A])(f: DBIO[A ⇒ B]): DBIO[B] =
      fa.flatMap(a ⇒ f.map(ff ⇒ ff(a)))

    def pure[A](a: A): DBIO[A] = DBIO.successful(a)
  }

  implicit def dbioMonad(implicit ec: EC) = new Functor[DBIO] with Monad[DBIO] {
    override def map[A, B](fa: DBIO[A])(f: A ⇒ B): DBIO[B] = fa.map(f)

    override def pure[A](a: A): DBIO[A] = DBIO.successful(a)

    override def flatMap[A, B](fa: DBIO[A])(f: A ⇒ DBIO[B]): DBIO[B] = fa.flatMap(f)
  }

  // implicits
  implicit class EnrichedDbResultT[A](dbResultT: DbResultT[A]) {
    def runTxn()(implicit db: DB): Result[A] =
      dbResultT.value.transactionally.run()

    def run()(implicit db: DB): Result[A] =
      dbResultT.value.run()

    def ignoreResult(implicit ec: EC): DbResultT[Unit] = for (_ ← * <~ dbResultT) yield {}
  }

  final implicit class EnrichedOption[A](val option: Option[A]) extends AnyVal {
    def toXor[F](or: F): F Xor A =
      option.fold { Xor.left[F, A](or) }(Xor.right[F, A])
  }

  // DbResultT
  object DbResultT {

    def apply[A](v: DBIO[Failures Xor A]): DbResultT[A] =
      XorT[DBIO, Failures, A](v)

    def pure[A](v: A)(implicit ec: EC): DbResultT[A] =
      XorT.pure[DBIO, Failures, A](v)

    def fromXor[A](v: Failures Xor A)(implicit ec: EC): DbResultT[A] =
      v.fold(failures, good)

    def fromDbio[A](v: DBIO[A])(implicit ec: EC): DbResultT[A] =
      XorT.right[DBIO, Failures, A](v)

    def good[A](v: A)(implicit ec: EC): DbResultT[A] =
      XorT.right[DBIO, Failures, A](DBIO.successful(v))

    def failures[A](v: Failures)(implicit ec: EC): DbResultT[A] =
      XorT.left[DBIO, Failures, A](DBIO.successful(v))

    def failure[A](f: Failure)(implicit ec: EC): DbResultT[A] =
      failures[A](f.single)

    def sequence[A, M[X] <: TraversableOnce[X]](values: M[DbResultT[A]])(
        implicit buildFrom: CanBuildFrom[M[DbResultT[A]], A, M[A]],
        ec: EC): DbResultT[M[A]] =
      values
        .foldLeft(good(buildFrom(values))) { (liftedBuilder, liftedValue) ⇒
          for (builder ← liftedBuilder; value ← liftedValue) yield builder += value
        }
        .map(_.result)

    def unit(implicit ec: EC): DbResultT[Unit] =
      pure({})

    def none[A](implicit ec: EC): DbResultT[Option[A]] =
      pure(Option.empty[A])
  }

  object * {
    def <~[A](v: DBIO[Failures Xor A]): DbResultT[A] =
      DbResultT(v)

    def <~[A](v: SqlAction[A, NoStream, Effect.All])(implicit ec: EC): DbResultT[A] =
      DbResultT(v.map(Xor.right))

    def <~[A](v: DBIO[A])(implicit ec: EC): DbResultT[A] =
      DbResultT.fromDbio(v)

    def <~[A](v: Failures Xor A)(implicit ec: EC): DbResultT[A] =
      DbResultT.fromXor(v)

    def <~[A](v: Future[Failures Xor A]): DbResultT[A] =
      DbResultT(DBIO.from(v))

    def <~[A](v: A)(implicit ec: EC): DbResultT[A] =
      DbResultT.pure(v)

    def <~[A](v: Validated[Failures, A])(implicit ec: EC): DbResultT[A] =
      DbResultT.fromXor(v.toXor)

    def <~[A, M[X] <: TraversableOnce[X]](v: M[DbResultT[A]])(
        implicit buildFrom: CanBuildFrom[M[DbResultT[A]], A, M[A]],
        ec: EC): DbResultT[M[A]] =
      DbResultT.sequence(v)

    def <~[A](v: DbResultT[A]): DbResultT[A] =
      v
  }

}
