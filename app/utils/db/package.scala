package utils

import scala.concurrent.Future

import cats.data.{Xor, XorT}

import failures._
import responses.{PaginationMetadata, SortingMetadata, TheResponse}
import slick.driver.PostgresDriver.api._
import slick.jdbc.SQLActionBuilder
import slick.lifted.{ColumnOrdered, Ordered, Query}
import slick.profile.{SqlAction, SqlStreamingAction}
import utils.http.CustomDirectives._
import utils.aliases._
import utils.db.DbResultT._
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

  case class QueryMetadata(sortBy: Option[String] = None,
                           from: Option[Int] = None,
                           size: Option[Int] = None,
                           pageNo: Option[Int] = None,
                           total: Option[DBIO[Int]] = None)

  object QueryMetadata {
    def empty = QueryMetadata()
  }

  case class ResponseMetadata(sortBy: Option[String] = None,
                              from: Option[Int] = None,
                              size: Option[Int] = None,
                              pageNo: Option[Int] = None,
                              total: Option[Int] = None)

  case class ResponseWithMetadata[A](result: Failures Xor A, metadata: ResponseMetadata)

  case class ResultWithMetadata[A](result: DbResult[A], metadata: QueryMetadata) {

    def wrapExceptions(implicit ec: EC): ResultWithMetadata[A] =
      this.copy(result = ExceptionWrapper.wrapDbResult(result))

    def map[S](f: A ⇒ S)(implicit ec: EC): ResultWithMetadata[S] =
      this.copy(result = result.map(_.map(f)))

    def flatMap[S](f: Failures Xor A ⇒ DbResult[S])(implicit ec: EC): ResultWithMetadata[S] =
      this.copy(result = result.flatMap(f))

    def toTheResponse(implicit ec: EC): DbResultT[TheResponse[A]] = {
      val pagingMetadata = PaginationMetadata(
          from = metadata.from, size = metadata.size, pageNo = metadata.pageNo)

      for {
        result ← * <~ this.result
        total  ← * <~ metadata.total.map(_.map(Some(_)).toXor).getOrElse(DbResult.none[Int])
      } yield
        TheResponse(result,
                    pagination = Some(pagingMetadata.copy(total = total)),
                    sorting = Some(SortingMetadata(sortBy = metadata.sortBy)))
    }
  }

  object ResultWithMetadata {
    def fromResultOnly[A](result: DbResult[A]): ResultWithMetadata[A] =
      ResultWithMetadata(result = result, metadata = QueryMetadata.empty)

    def fromFailures[A](failures: Failures): ResultWithMetadata[A] =
      ResultWithMetadata(result = DbResult.failures(failures), metadata = QueryMetadata.empty)
  }

  private def _paged[E, U, C[_]](query: Query[E, U, C])(implicit sortAndPage: SortAndPage) = {
    val pagedQueryOpt = for {
      from ← sortAndPage.from
      size ← sortAndPage.size
    } yield query.drop(from).take(size)

    pagedQueryOpt.getOrElse(query)
  }

  def invalidSortColumn(name: String): ColumnOrdered[AnyRef] = {
    throw new IllegalArgumentException(s"Invalid sort column: $name")
  }

  case class QueryWithMetadata[E, U, C[_]](query: Query[E, U, C], metadata: QueryMetadata) {

    def sortBy(f: E ⇒ Ordered): QueryWithMetadata[E, U, C] =
      this.copy(query = query.sortBy(f))

    def sortIfNeeded(
        f: (Sort, E) ⇒ Ordered)(implicit sortAndPage: SortAndPage): QueryWithMetadata[E, U, C] =
      sortAndPage.sort match {
        case Some(s) ⇒ this.copy(query = query.sortBy(f.curried(s)))
        case None    ⇒ this
      }

    def sortAndPageIfNeeded(f: (Sort, E) ⇒ Ordered)(
        implicit sortAndPage: SortAndPage): QueryWithMetadata[E, U, C] = sortIfNeeded(f).paged

    def paged(implicit sortAndPage: SortAndPage): QueryWithMetadata[E, U, C] =
      this.copy(query = _paged(query))

    def result(implicit ec: EC): ResultWithMetadata[C[U]] =
      ResultWithMetadata(result = DbResult.fromDbio(query.result), metadata)
  }

  implicit class EnrichedQuery[E, U, C[_]](val query: Query[E, U, C]) extends AnyVal {
    def one: DBIO[Option[U]] = query.result.headOption

    def mustFindOneOr(notFoundFailure: Failure)(implicit ec: EC): DbResult[U] =
      query.one.mustFindOr(notFoundFailure)

    def paged(implicit sortAndPage: SortAndPage): Query[E, U, C] = _paged(query)

    def withEmptyMetadata: QueryWithMetadata[E, U, C] =
      QueryWithMetadata(query, QueryMetadata.empty)

    def withMetadata(metadata: QueryMetadata): QueryWithMetadata[E, U, C] =
      QueryWithMetadata(query, metadata)

    def withMetadata(implicit sortAndPage: SortAndPage): QueryWithMetadata[E, U, C] = {

      val from = sortAndPage.from.getOrElse(0)
      val size = sortAndPage.size.getOrElse(DefaultPageSize)

      // size > 0 costraint is defined in SortAndPage
      val pageNo = (from / size) + 1

      val metadata = QueryMetadata(sortBy = sortAndPage.sortBy,
                                   from = Some(from),
                                   size = Some(size),
                                   pageNo = Some(pageNo),
                                   total = Some(query.length.result))

      withMetadata(metadata)
    }
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

  def xorMapDbio[LeftX, RightX, RightY](xor: Xor[LeftX, RightX])(
      f: RightX ⇒ DBIO[RightY])(implicit ec: EC): DBIO[Xor[LeftX, RightY]] = {
    xor.fold(
        fs ⇒ lift(Xor.left(fs)),
        v ⇒ f(v).map(Xor.right)
    )
  }
}
