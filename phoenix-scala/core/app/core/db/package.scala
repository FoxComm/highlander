package core

import cats._
import cats.data._
import cats.implicits._
import core.failures.{Failure, Failures, _}
import core.utils.Money
import core.utils.time.JavaTimeSlickMapper
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.SQLActionBuilder
import slick.lifted.Query
import slick.sql.SqlAction

import scala.concurrent.{ExecutionContext, Future}

/*_*/ // <- this little guy will disable IJ lint for the code below
package object db {

  type EC = ExecutionContext
  type DB = slick.jdbc.PostgresProfile.api.Database

  // ————————————————————————————— Foxy —————————————————————————————

  sealed trait MetaResponse
  object MetaResponse {

    /** Warnings that will get shown to the user and can be appended
      * anywhere in the processing flow. */
    final case class Warning(ζ: Failure) extends MetaResponse

    /** Non-fatal errors that will get shown to the user. As opposed to
      * Failures, which short-circuit everything and result in an HTTP
      * error, this can be used to signal that *some* requested jobs
      * have failed, but others did fine, and here’s what we were able
      * to achieve anyway, and here are the errors encountered. */
    final case class Error(ζ: Failure) extends MetaResponse
    // FIXME: uncomment this & pull BatchMetadata from phoenix into starfish, when getting rid of TheResponse @michalrus
    /* final case class BatchInfo(ζ: BatchMetadata) extends MetaResponse */
  }

  /* We can’t use WriterT for warnings, because of the `failWithMatchedWarning`. */
  type FoxyT[F[_], A] = StateT[EitherT[F, Failures, ?], List[MetaResponse], A] // TODO: But maybe the order should be different? I.e. what should happen with warnings when we get a short-circuiting failure? @michalrus

  object FoxyT {
    def apply[F[_]]: FoxyTFunctions[F] = new FoxyTFunctions[F] {}
  }

  // FIXME: Have this derived automatically, after updating Cats. @michalrus
  implicit def foxyTMonadError[F[_]](implicit F: Monad[F],
                                     M: Monad[FoxyT[F, ?]]): MonadError[FoxyT[F, ?], Failures] =
    new MonadError[FoxyT[F, ?], Failures] {
      override def raiseError[A](e: Failures): FoxyT[F, A] =
        FoxyT[F].failures(e)

      override def handleErrorWith[A](fa: FoxyT[F, A])(f: (Failures) ⇒ FoxyT[F, A]): FoxyT[F, A] =
        fa.transformF { fsa ⇒
          EitherT(F.flatMap(fsa.value) { xsa ⇒
            xsa match {
              case Left(failures) ⇒
                f(failures).runEmpty.value // TODO: Re-think discarding warnings. @michalrus
              case r ⇒ F.pure(r)
            }
          })
        }

      // TODO: How to move definitions around to get rid of the three proxies below? https://gitter.im/typelevel/cats?at=58b954c77ceae5376a6688be @michalrus
      override def pure[A](x: A): FoxyT[F, A] = M.pure(x)
      override def flatMap[A, B](fa: FoxyT[F, A])(f: (A) ⇒ FoxyT[F, B]): FoxyT[F, B] =
        M.flatMap(fa)(f)
      override def tailRecM[A, B](a: A)(f: (A) ⇒ FoxyT[F, Either[A, B]]): FoxyT[F, B] =
        M.tailRecM(a)(f)
    }

  implicit class FoxyTOps[F[_], A](private val fa: FoxyT[F, A]) extends AnyVal {
    // TODO: Remove directly relying Either[on, in] the rest of the codebase iteratively and completely. @michalrus

    def flatMapEither[B](f: Either[Failures, A] ⇒ FoxyT[F, B])(implicit F: Monad[F]): FoxyT[F, B] = // TODO: remove me @michalrus
      fa.flatMap(a ⇒ f(Either.right(a))).handleErrorWith(failures ⇒ f(Either.left(failures)))

    def mapEither[B](f: Either[Failures, A] ⇒ Either[Failures, B])(implicit F: Monad[F]): FoxyT[F, B] = // TODO: remove me @michalrus
      flatMapEither(xa ⇒ FoxyT[F].fromEither(f(xa)))

    def mapEitherRight[B](f: Either[Failures, A] ⇒ B)(implicit F: Monad[F]): FoxyT[F, B] =
      mapEither(xa ⇒ Either.right(f(xa))) // TODO: remove me @michalrus

    def fold[B](ra: Failures ⇒ B, rb: A ⇒ B)(implicit F: Monad[F]): FoxyT[F, B] = // TODO: this is not fold… Find a better name or remove it? @michalrus
      fa.map(rb).handleError(ra)

    def meh(implicit M: Monad[F]): FoxyT[F, Unit] =
      fa.void // TODO: remove me? But it’s cute… @michalrus

    def failuresToWarnings(valueIfWasFailed: A)(pf: PartialFunction[Failure, Boolean])(
        implicit F: Monad[F]): FoxyT[F, A] =
      fa.handleErrorWith { fs ⇒
        val lpf                  = pf.lift
        val (warnings, failures) = fs.toList.partition(lpf(_) == Some(true))
        failures match {
          case h :: t ⇒
            // We don’t care about warnings when there’re failures left.
            FoxyT[F].failures[A](NonEmptyList(h, t))
          case Nil ⇒
            warnings.traverse(FoxyT[F].uiWarning).map(_ ⇒ valueIfWasFailed)
        }
      }
  }

  trait FoxyTFunctions[F[_]] {
    def good[A](a: A)(implicit F: Monad[F]): FoxyT[F, A] = // TODO: remove me @michalrus
      a.pure[FoxyT[F, ?]]

    def unit(implicit F: Monad[F]): FoxyT[F, Unit] =
      ().pure[FoxyT[F, ?]] // TODO: remove me? @michalrus

    def none[A](implicit F: Monad[F]): FoxyT[F, Option[A]] =
      (None: Option[A]).pure[FoxyT[F, ?]] // TODO: remove me? @michalrus

    def uiWarning(f: Failure)(implicit F: Monad[F]): FoxyT[F, Unit] =
      StateT.modify(MetaResponse.Warning(f) :: _)

    def uiError(f: Failure)(implicit F: Monad[F]): FoxyT[F, Unit] =
      StateT.modify(MetaResponse.Error(f) :: _)

    def failures[A](f: Failures)(implicit F: Monad[F]): FoxyT[F, A] = // TODO: shouldn’t A =:= Unit? @michalrus
      StateT(_ ⇒ EitherT.left(F.pure(f)))

    def failure[A](f: Failure)(implicit F: Monad[F]): FoxyT[F, A] = // TODO: remove me? @michalrus
      failures(f.single)

    def fromF[A](fa: F[A])(implicit F: Monad[F]): FoxyT[F, A] = // TODO: better name? @michalrus
      StateT(s ⇒ EitherT.right(F.map(fa)((s, _))))

    def fromEither[A](v: Either[Failures, A])(implicit F: Monad[F]): FoxyT[F, A] = // TODO: remove me @michalrus
      StateT(s ⇒ EitherT(F.pure(v.map((s, _)))))

    def fromFEither[A](v: F[Either[Failures, A]])(implicit F: Monad[F]): FoxyT[F, A] = // TODO: remove me @michalrus
      StateT(s ⇒ EitherT(F.map(v)(_.map((s, _)))))

    def fromId[A](fa: Foxy[A])(implicit F: Monad[F]): FoxyT[F, A] =
      fa.transformF(ga ⇒ EitherT(F.pure(ga.value)))

    def failWithMatchedWarning(pf: PartialFunction[Failure, Boolean])(implicit F: Monad[F]): FoxyT[F, Unit] =
      StateT(
        s ⇒
          s.collect {
              case MetaResponse.Warning(f) ⇒ f
            }
            .find(pf.lift(_) == Some(true)) match {
            case Some(f) ⇒ EitherT.left(F.pure(NonEmptyList(f, Nil)))
            case _       ⇒ EitherT.right(F.pure((s, ())))
        })

    /** Just like ``sequence`` but—in case of a failure—unlawful, as it will join failures from all Foxies. */
    def seqCollectFailures[L[_], A](lfa: L[FoxyT[F, A]])(implicit L: TraverseFilter[L],
                                                         F: Monad[F]): FoxyT[F, L[A]] =
      L.map(lfa)(_.fold(Either.left(_), Either.right(_))).sequence.flatMap { xa ⇒
        val failures = L.collect(xa) { case Left(f) ⇒ f.toList }.toList.flatten
        val values   = L.collect(xa) { case Right(a) ⇒ a }
        NonEmptyList.fromList(failures).fold(values.pure[FoxyT[F, ?]])(FoxyT[F].failures(_))
      }

    /** A bit like ``sequence`` but will ignore failed Foxies. */
    // TODO: is this useful enough to have in FoxyT? @michalrus
    def onlySuccessful[L[_], A](xs: L[FoxyT[F, A]])(implicit L: TraverseFilter[L],
                                                    M: Monad[F]): FoxyT[F, L[A]] =
      for {
        xs ← xs.map(_.map(_.some).recover { case _ ⇒ None }).sequence
      } yield xs.collect { case Some(xss) ⇒ xss }

    /** Like `seqCollectFailures` but you get a chance to recover some Failures, changing them to Warnings. */
    def seqFailuresToWarnings[L[_], A](xs: L[FoxyT[F, A]],
                                       failuresToWarningsPF: PartialFunction[Failure, Boolean])(
        implicit L: TraverseFilter[L],
        F: Monad[F]): FoxyT[F, L[A]] =
      seqCollectFailures(xs.map(_.map(_.some).failuresToWarnings(None)(failuresToWarningsPF)))
        .map(_.flattenOption)
  }

  implicit class FoxyTSeqOps[L[_]: TraverseFilter, F[_]: Monad, A](xs: L[FoxyT[F, A]]) {
    def ignoreFailures: FoxyT[F, L[A]] = FoxyT[F].onlySuccessful(xs)
  }

  // ————————————————————————————— Foxy: aliases —————————————————————————————

  type Foxy[A] = FoxyT[Id, A]
  val Foxy = FoxyT[Id]

  type FoxyTFuture[A] = FoxyT[Future, A] /* replaces the old ResultT */
  val FoxyTFuture = FoxyT[Future]

  type FoxyTDBIO[A] = FoxyT[DBIO, A] /* replaces the old DbResultT */
  object FoxyTDBIO extends FoxyTFunctions[DBIO] {
    def fromResult[A](ga: FoxyT[Future, A])(implicit F: Monad[Future], G: Monad[DBIO]): FoxyT[DBIO, A] = // TODO: better name? @michalrus
      // Don’t remove type annotation below, or the compiler will crash. 🙄
      ga.transformF(gga ⇒ EitherT(DBIO.from(gga.value): DBIO[Either[Failures, (List[MetaResponse], A)]])) // TODO: use FunctionK for functor changes? Future[_] → DBIO[_] here
  }

  type DbResultT[A] = FoxyTDBIO[A]
  val DbResultT = FoxyTDBIO

  type Result[A] = FoxyTFuture[A]
  val Result = FoxyTFuture

  // ————————————————————————————— /Foxy —————————————————————————————

  // DBIO monad
  implicit def dbioMonad(implicit ec: EC): Functor[DBIO] with Monad[DBIO] with Applicative[DBIO] =
    new Functor[DBIO] with Monad[DBIO] with Applicative[DBIO] {
      override def ap[A, B](f: DBIO[A ⇒ B])(fa: DBIO[A]): DBIO[B] =
        fa.flatMap(a ⇒ f.map(ff ⇒ ff(a)))

      override def map[A, B](fa: DBIO[A])(f: A ⇒ B): DBIO[B] = fa.map(f)

      override def pure[A](a: A): DBIO[A] = DBIO.successful(a)

      override def flatMap[A, B](fa: DBIO[A])(f: A ⇒ DBIO[B]): DBIO[B] = fa.flatMap(f)

      override def tailRecM[A, B](a: A)(f: A ⇒ DBIO[Either[A, B]]): DBIO[B] =
        flatMap(f(a)) {
          case Right(b)    ⇒ pure(b)
          case Left(nextA) ⇒ tailRecM(nextA)(f)
        }
    }

  // implicits
  implicit class EnrichedDbResultT[A](dbResultT: DbResultT[A]) {
    def runTxn()(implicit ec: EC, db: DB): Result[A] = // FIXME: this should be doable without all this ceremony, supplying .transactionally.run to some standard function @michalrus
      dbResultT.transformF(
        fsa ⇒
          EitherT(
            fsa
            // turn `left` into `DBIO.failed` to force transaction rollback
              .fold(failures ⇒ DBIO.failed(FoxFailureException(failures)), good ⇒ DBIO.successful(good))
              .flatMap(a ⇒ a) // flatten...
              .transactionally
              .run() // throws a FoxFailureException :/
              .map(Either.right)
              .recover { // don't actually want an exception thrown, so wrap it back
                case e: FoxFailureException ⇒ Either.left(e.failures)
              }))

    def runDBIO()(implicit ec: EC, db: DB): Result[A] =
      dbResultT.transformF(fa ⇒ EitherT(fa.value.run))

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
    def toEither[F](or: F): Either[F, A] =
      option.fold { Either.left[F, A](or) }(Either.right[F, A])
  }

  // Return B whenever A is inserted
  type Returning[A, B] = slick.jdbc.JdbcActionComponent#ReturningInsertActionComposer[A, B]

  implicit val javaTimeSlickMapper      = JavaTimeSlickMapper.instantAndTimestampWithoutZone
  implicit val currencyColumnTypeMapper = Money.currencyColumnType

  abstract class FoxTable[M <: FoxModel[M]](tag: Tag, name: String) extends Table[M](tag, name) {
    def id: Rep[M#Id]
  }

  def appendForUpdate[A, B <: slick.dbio.NoStream](sql: SqlAction[A, B, Effect.Read]): DBIO[A] =
    sql.overrideStatements(sql.statements.map(_ + " for update"))

  // TODO: I don’t know… does this help? @michalrus
  def ifElse[A](condition: Boolean, ifBranch: ⇒ DbResultT[A], elseBranch: ⇒ DbResultT[A]) =
    if (condition) ifBranch else elseBranch

  def when[F[_]](p: Boolean, s: ⇒ F[Unit])(implicit F: Applicative[F]): F[Unit] =
    if (p) s.void else F.pure(())

  def doOrGood[A](condition: Boolean, action: ⇒ DbResultT[A], good: ⇒ A)(implicit ec: EC): DbResultT[A] =
    if (condition) action else DbResultT.good(good)

  def doOrFail[A](condition: Boolean, action: ⇒ DbResultT[A], failure: ⇒ Failure)(
      implicit ec: EC): DbResultT[A] =
    if (condition) action else DbResultT.failure(failure)

  def failIf(condition: Boolean, failure: ⇒ Failure)(implicit ec: EC): DbResultT[Unit] =
    if (condition) DbResultT.failure(failure) else DbResultT.unit

  def failIfNot(condition: Boolean, failure: ⇒ Failure)(implicit ec: EC): DbResultT[Unit] =
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

    def findOrCreate(r: DbResultT[R])(implicit ec: EC): DbResultT[R] =
      dbio.dbresult.flatMap {
        case Some(model) ⇒ DbResultT.good(model)
        case None        ⇒ r
      }

    // Last item in tuple determines if cart was created or not
    def findOrCreateExtended(r: DbResultT[R])(implicit ec: EC): DbResultT[(R, FoundOrCreated)] =
      dbio.dbresult.flatMap {
        case Some(model) ⇒ DbResultT.good((model, Found))
        case _           ⇒ r.map(result ⇒ (result, Created))
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
    // FIXME: if you know it, prove it. Or s/safe/unsafe/ in the name *AND* comment. @michalrus
    def safeGet(implicit ec: EC): DBIO[R] = dbio.map(_.get)
  }
}
