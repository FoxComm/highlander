package utils

import scala.concurrent.Future
import cats._
import cats.data._
import cats.implicits._
import failures._
import responses.BatchMetadata
import services.Result
import slick.driver.PostgresDriver.api._
import slick.jdbc.SQLActionBuilder
import slick.lifted.Query
import slick.profile.SqlAction
import utils.aliases._
import utils.time.JavaTimeSlickMapper

import scala.collection.generic.CanBuildFrom

package object db {

  // ————————————————————————————— Foxy —————————————————————————————

  //final case class Failure(ζ: String) extends AnyVal

  sealed trait UIInfo
  object UIInfo {
    final case class Warning(ζ: Failure)         extends UIInfo
    final case class BatchInfo(ζ: BatchMetadata) extends UIInfo
  }

  /* We can’t use WriterT for warnings, because of the `failWithMatchedWarning`. */
  type FoxyT[F[_], A] = StateT[XorT[F, Failures, ?], List[UIInfo], A]

  type Foxy[A] = FoxyT[Id, A]
  object Foxy extends FoxyTOps[Id]()

  type FoxyTFuture[A] = FoxyT[Future, A] /* replaces the old ResultT */
  object FoxyTFuture extends FoxyTOps[Future]

  type FoxyTDBIO[A] = FoxyT[DBIO, A] /* replaces the old DbResultT */
  object FoxyTDBIO extends FoxyTOps[DBIO] {
    // FIXME: make it use cats.Foldable instead and move to FoxyTOps @michalrus
    def sequence[A, M[X] <: TraversableOnce[X]](values: M[DbResultT[A]])(
        implicit buildFrom: CanBuildFrom[M[DbResultT[A]], A, M[A]],
        ec: EC): DbResultT[M[A]] =
      values
        .foldLeft(good(buildFrom(values))) { (liftedBuilder, liftedValue) ⇒
          for (builder ← liftedBuilder; value ← liftedValue) yield builder += value
        }
        .map(_.result)
  }

  implicit class EnrichedFoxyT[F[_], A](fa: FoxyT[F, A]) {
    def flatMapXor[A](f: Xor[Failures, A] ⇒ FoxyT[F, A]): FoxyT[F, A] =
      ??? // FIXME: implement // TODO: remove me? @michalrus
  }

  trait FoxyTOps[F[_]] {
    def apply[A](a: A)(implicit M: Monad[F]): FoxyT[F, A] = // TODO: remove me? @michalrus
      pure(a)

    def pure[A](a: A)(implicit M: Monad[F]): FoxyT[F, A] =
      Monad[FoxyT[F, ?]].pure(a)

    def good[A](a: A)(implicit M: Monad[F]): FoxyT[F, A] = // TODO: remove me @michalrus
      pure(a)

    def unit(implicit M: Monad[F]): FoxyT[F, Unit] = pure(()) // TODO: remove me? @michalrus

    def none[A](implicit M: Monad[F]): FoxyT[F, Option[A]] =
      pure(None) // TODO: remove me? @michalrus

    def warning(f: Failure)(implicit M: Monad[F]): FoxyT[F, Unit] =
      StateT.modify(UIInfo.Warning(f) :: _)

    def failures[A](f: Failures)(implicit M: Monad[F]): FoxyT[F, A] = // TODO: shouldn’t A =:= Unit? @michalrus
      StateT(_ ⇒ XorT.left(M.pure(f)))

    def failure[A](f: Failure)(implicit M: Monad[F]): FoxyT[F, A] = // TODO: remove me? @michalrus
      failures(f.single)

    def fromF[A](fa: F[A])(implicit M: Monad[F]): FoxyT[F, A] = // TODO: better name? @michalrus
      StateT(s ⇒ XorT.right(M.map(fa)((s, _))))

    def fromXor[A](v: Failures Xor A)(implicit M: Monad[F]): FoxyT[F, A] =
      StateT(s ⇒ XorT(M.pure(v)))

    def fromG[G[_], A, B](f: G[B] ⇒ F[B], ga: FoxyT[G, A]): FoxyT[F, A] = // TODO: better name? @michalrus
      ga.transformF(gga ⇒ XorT(f(gga.value)))

    def fromId[A](fa: Foxy[A])(implicit M: Monad[F]): FoxyT[F, A] =
      //fa.transformF(ga ⇒ XorT(M.pure(ga.value)))
      fromG(M.pure, fa)

    def failWithMatchedWarning(pf: PartialFunction[Failure, Boolean])(
        implicit M: Monad[F]): FoxyT[F, Unit] =
      StateT(s ⇒
            s.collect {
          case UIInfo.Warning(f) ⇒ f
        }.find(pf.lift(_) == Some(true)) match {
          case Some(f) ⇒ XorT.left(M.pure(NonEmptyList(f, Nil)))
          case _       ⇒ XorT.right(M.pure((s, ())))
      })

  }

  // ————————————————————————————— /Foxy —————————————————————————————

  type DbResultT[A] = FoxyTDBIO[A]
  val DbResultT = FoxyTDBIO

  // DBIO monad
  implicit def dbioMonad(implicit ec: EC): Functor[DBIO] with Monad[DBIO] with Applicative[DBIO] =
    new Functor[DBIO] with Monad[DBIO] with Applicative[DBIO] {
      override def ap[A, B](f: DBIO[A ⇒ B])(fa: DBIO[A]): DBIO[B] =
        fa.flatMap(a ⇒ f.map(ff ⇒ ff(a)))

      override def map[A, B](fa: DBIO[A])(f: A ⇒ B): DBIO[B] = fa.map(f)

      override def pure[A](a: A): DBIO[A] = DBIO.successful(a)

      override def flatMap[A, B](fa: DBIO[A])(f: A ⇒ DBIO[B]): DBIO[B] = fa.flatMap(f)

      override def tailRecM[A, B](a: A)(f: A ⇒ DBIO[Either[A, B]]): DBIO[B] =
        defaultTailRecM(a)(f)
    }

  // implicits
  implicit class EnrichedDbResultT[A](dbResultT: DbResultT[A]) {
    def runTxn()(implicit ec: EC, db: DB): Result[A] =
      dbResultT.transformF(
          fsa ⇒
            XorT(
                fsa
                  .fold(failures ⇒ DBIO.failed(FoxFailureException(failures)),
                        good ⇒ DBIO.successful(good))
                  .flatMap(a ⇒ a) // flatten...
                  .transactionally
                  .run
                  .map(Xor.right)
                  .recover {
            case e: FoxFailureException ⇒ Xor.left(e.failures)
          }))

    def runDBIO()(implicit ec: EC, db: DB): Result[A] = {
      //val F19 = FlatMap[DBIO](dbioMonad)
      //dbResultT.value.run()
      dbResultT.transformF(fa ⇒ XorT(fa.value.run))
    }

    def meh(implicit ec: EC): DbResultT[Unit] = for (_ ← * <~ dbResultT) yield {}

    def resolveFailures(resolver: PartialFunction[Failure, Failure])(
        implicit ec: EC): DbResultT[A] = {
      def mapFailure(failure: Failure) = resolver.applyOrElse(failure, identity[Failure])

      dbResultT.transformF(_.leftMap {
        case NonEmptyList(h, t) ⇒ NonEmptyList(mapFailure(h), t.map(mapFailure))
      })
    }
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
    if (condition) action else DbResultT.failure(failure)

  def failIf(condition: Boolean, failure: Failure)(implicit ec: EC): DbResultT[Unit] =
    if (condition) DbResultT.failure(failure) else DbResultT.unit

  def failIfNot(condition: Boolean, failure: Failure)(implicit ec: EC): DbResultT[Unit] =
    failIf(!condition, failure)

  def failIfFailures(failures: Seq[Failure])(implicit ec: EC): DbResultT[Unit] =
    failures match {
      case head :: tail ⇒
        DbResultT.failures(NonEmptyList.of(head, tail: _*))
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
      DbResultT.fromF(dbio)
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
