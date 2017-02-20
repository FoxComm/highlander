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

  sealed trait UIInfo
  object UIInfo {
    final case class Warning(ζ: Failure)         extends UIInfo
    final case class BatchInfo(ζ: BatchMetadata) extends UIInfo
  }

  /* We can’t use WriterT for warnings, because of the `failWithMatchedWarning`. */
  type FoxyT[F[_], A] = StateT[XorT[F, Failures, ?], List[UIInfo], A] // TODO: But maybe the order should be different? I.e. what should happen with warnings when we get a short-circuiting failure? @michalrus

  type Foxy[A] = FoxyT[Id, A]
  object Foxy extends FoxyTOps[Id]()

  type FoxyTFuture[A] = FoxyT[Future, A] /* replaces the old ResultT */
  object FoxyTFuture extends FoxyTOps[Future] {
    def fromFutureXor[A](v: Future[Failures Xor A])(implicit M: Monad[Future]): FoxyT[Future, A] = // TODO: remove me @michalrus
      StateT(s ⇒ XorT(M.map(v)(_.map((s, _)))))
  }

  type FoxyTDBIO[A] = FoxyT[DBIO, A] /* replaces the old DbResultT */
  object FoxyTDBIO extends FoxyTOps[DBIO] {
    def fromDbio[A](fa: DBIO[A])(implicit M: Monad[DBIO]): FoxyTDBIO[A] = // TODO: remove me @michalrus
      fromF(fa)
    def fromResultT[A](ga: FoxyT[Future, A]): FoxyT[DBIO, A] = // TODO: better name? @michalrus
      ga.transformF(gga ⇒ XorT(DBIO.from(gga.value): DBIO[A])) // TODO: use FunctionK for functor changes? Future[_] → DBIO[_] here
  }

  implicit class EnrichedFoxyT[F[_], A](fa: FoxyT[F, A]) {
    def flatMapXor[B](f: Xor[Failures, A] ⇒ FoxyT[F, B]): FoxyT[F, B] =
      ??? // FIXME: implement // TODO: remove me? @michalrus
    def mapXor[B](f: Xor[Failures, A] ⇒ Xor[Failures, B])(implicit F: Monad[F]): FoxyT[F, B] = // TODO: remove me? @michalrus
      fa.transformF { fsa ⇒ // FIXME: this (or usage in CustomerDynamicGroupQualifier) seems to be crashing compiler with scala.reflect.internal.Types$NoCommonType: lub/glb of incompatible types: [_] and  <: slick.dbio.NoStream
        XorT(F.map(fsa.value)(xsa ⇒
                  for {
            b ← f(xsa.map(_._2))
            s ← xsa.map(_._1)
          } yield (s, b)))
      }
    def mapXorRight[B](f: Xor[Failures, A] ⇒ B): FoxyT[F, B] =
      mapXor(xa ⇒ Xor.Right(f(xa))) // TODO: remove me? @michalrus
    def fold[B](fa: Failures ⇒ B, fb: A ⇒ B): FoxyT[F, B] =
      ??? // FIXME: implement
    def recoverWith(pf: PartialFunction[Failure, FoxyT[F, A]]): FoxyT[F, A] =
      ??? // FIXME: implement // (this one is actually useful)
    def recover(pf: PartialFunction[Failure, A]): FoxyT[F, A] =
      ??? // FIXME: implement // (this one is actually useful)
    def meh(implicit M: Monad[F]): FoxyT[F, Unit] = for (_ ← fa) yield {}
    def failuresToWarnings(pf: PartialFunction[Failure, Boolean]): FoxyT[F, A] =
      ??? // FIXME: implement
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
      StateT(s ⇒ XorT(M.pure(v.map((s, _)))))

    def fromId[A](fa: Foxy[A])(implicit M: Monad[F]): FoxyT[F, A] =
      fa.transformF(ga ⇒ XorT(M.pure(ga.value)))

    def failWithMatchedWarning(pf: PartialFunction[Failure, Boolean])(
        implicit M: Monad[F]): FoxyT[F, Unit] =
      StateT(s ⇒
            s.collect {
          case UIInfo.Warning(f) ⇒ f
        }.find(pf.lift(_) == Some(true)) match {
          case Some(f) ⇒ XorT.left(M.pure(NonEmptyList(f, Nil)))
          case _       ⇒ XorT.right(M.pure((s, ())))
      })

    // FIXME: make it use cats.Foldable instead and move to FoxyTOps @michalrus
    def sequence[A, M[X] <: TraversableOnce[X]](values: M[FoxyT[F, A]])(
        implicit buildFrom: CanBuildFrom[M[FoxyT[F, A]], A, M[A]]): FoxyT[F, M[A]] =
      ??? // TODO: IMPORTANT: append all Failures from Lefts in the final result, if there’s at least one Left. OfferList#adjust depends on that. And we’re not swallowing errors, then. @michalrus
    //      values
    //        .foldLeft(good(buildFrom(values))) { (liftedBuilder, liftedValue) ⇒
    //          for (builder ← liftedBuilder; value ← liftedValue) yield builder += value
    //        }
    //        .map(_.result)

    // TODO: is this useful enough to have in FoxyT? @michalrus
    def onlySuccessful[A](xs: Seq[FoxyT[F, A]])(implicit M: Monad[F]): FoxyT[F, Seq[A]] = // FIXME: get rid of explicit Seq @michalrus
      for {
        xs ← sequence(xs.map(_.map(_.some).recover { case _ ⇒ None }))
      } yield xs.collect { case Some(xss) ⇒ xss }
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
    def runTxn()(implicit ec: EC, db: DB): Result[A] = // FIXME: this should be doable without all this ceremony, supplying .transactionally.run to some standard function @michalrus
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

    def resolveFailures(
        resolver: PartialFunction[Failure, Failure])( // TODO: what’s that? Move to FoxyT. @michalrus
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
