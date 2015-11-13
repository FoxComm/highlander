package utils

import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

import cats.data.Xor
import models.Orders
import responses.FullOrder
import services.{Failure, Failures, Result, GeneralFailure}
import slick.ast._
import slick.driver.PostgresDriver._
import slick.driver.PostgresDriver.api._
import slick.jdbc.{ResultSetInvoker, SQLActionBuilder, GetResult, JdbcResultConverterDomain, SetParameter}

import slick.lifted.{ColumnOrdered, Ordered, Query}
import slick.profile.{SqlAction, SqlStreamingAction}
import slick.relational.{CompiledMapping, ResultConverter}
import slick.util.SQLBuilder
import utils.CustomDirectives.{Sort, SortAndPage}
import utils.DbResultT.DbResultT
import utils.ExceptionWrapper._

object Slick {

  type DbResult[T] = DBIO[Failures Xor T]

  def xorMapDbio[LeftX, RightX, RightY](xor: Xor[LeftX, RightX])(f: RightX ⇒ DBIO[RightY])
    (implicit ec: ExecutionContext): DBIO[Xor[LeftX, RightY]] = {
    xor.fold(
      fs ⇒ lift(Xor.left(fs)),
      v  ⇒ f(v).map(Xor.right)
    )
  }

  def appendForUpdate[A, B <: slick.dbio.NoStream](sql: SqlAction[A, B, Effect.Read]): DBIO[A] = {
    sql.overrideStatements(sql.statements.map(_ + " for update"))
  }

  def lift[A](value: A): DBIO[A] = DBIO.successful(value)

  def liftFuture[A](future: Future[A]): DBIO[A] = DBIO.from(future)

  def fullOrder(finder: Orders.QuerySeq)(implicit ec: ExecutionContext, db: Database): DBIO[FullOrder.Root] = {
    finder.result.head.flatMap(FullOrder.fromOrder)
  }

  object DbResult {

    val unit: DbResult[Unit] = DBIO.successful(Xor.right(Unit))

    def fromXor[A](xor: Failures Xor A): DbResult[A] = xor.fold(failures, good)

    def good[A](v: A): DbResult[A] = lift(Xor.right(v))

    def fromDbio[A](dbio: DBIO[A])(implicit ec: ExecutionContext): DbResult[A] = dbio.map(Xor.right)

    def fromFuture[A](future: Future[A])(implicit ec: ExecutionContext): DbResult[A] = fromDbio(liftFuture(future))

    def failure[A](failure: Failure): DbResult[A] = liftFuture(Result.failures(failure))

    def failures[A](failures: Failures): DbResult[A] = liftFuture(Result.failures(failures))
  }

  /*
    Provides an implicit conversion to allow for UDPATE _ RETURNING _ queries
    Usage: Customers.filter(_.id === 1).map(_.firstName).
      updateReturning(Customers.map(_.firstName), ("blah"))

    This was generously copied from and upgraded to Slick 3.0 from: http://stackoverflow.com/a/28148606/310275
   */
  object UpdateReturning {
    val columnRegex: Regex = "(\".*\")".r

    implicit class UpdateReturningInvoker[E, U, C[_]](val updateQuery: Query[E, U, C]) extends AnyVal {

      def updateReturningHead[A, F](returningQuery: Query[A, F, C], v: U)
        (implicit ec: ExecutionContext, db: Database): DbResult[F] =
        wrapDbio(updateReturning(returningQuery, v).head)

      def updateReturningHeadOption[A, F](returningQuery: Query[A, F, C], v: U, notFoundFailure: Failure)
        (implicit ec: ExecutionContext, db: Database): DbResult[F] =
        wrapDbResult(updateReturning(returningQuery, v).headOption
          .map(res ⇒ Xor.fromOption(res, notFoundFailure.single)))

      @SuppressWarnings(Array("org.brianmckenna.wartremover.warts.Any",
        "org.brianmckenna.wartremover.warts.IsInstanceOf", "org.brianmckenna.wartremover.warts.AsInstanceOf"))
      private def updateReturning[A, F](returningQuery: Query[A, F, C], v: U)
        (implicit ec: ExecutionContext, db: Database): SqlStreamingAction[Vector[F], F, Effect.All] = {
        val ResultSetMapping(_,
          CompiledStatement(_, sres: SQLBuilder.Result, _),
          CompiledMapping(_updateConverter, _)) = updateCompiler.run(updateQuery.toNode).tree

        val pconv: SetParameter[U] = {
          val ResultSetMapping(_, compiled, CompiledMapping(_converter, _)) = updateCompiler.run(updateQuery.toNode).tree
          val converter = _converter.asInstanceOf[ResultConverter[JdbcResultConverterDomain, U]]
          SetParameter[U] { (value, params) ⇒
            converter.set(value, params.ps)
          }
        }

        // extract the result/converter to build our RETURNING {columns} and re-use it for result conversion
        val ResultSetMapping(_,
        CompiledStatement(_, returningResult: SQLBuilder.Result, _),
        CompiledMapping(resultConverter, _)) = queryCompiler.run(returningQuery.toNode).tree

        val rconv: GetResult[F] = {
          val converter = resultConverter.asInstanceOf[ResultConverter[JdbcResultConverterDomain, F]]
          GetResult[F] { p ⇒
            converter.read(p.rs)
          }
        }

        // extract columns from the `SELECT {columns} FROM {table}` after dropping `FROM .*` from query str
        val columns = columnRegex.findAllIn(returningResult.sql.replaceAll(" from .*", "")).toList
        val fieldsExp = columns.mkString(", ")
        val returningSql = sres.sql + s" RETURNING $fieldsExp"

        SQLActionBuilder(returningSql, pconv.applied(v)).as[F](rconv)
      }
    }
  }

  object implicits {
    implicit class EnrichedSQLActionBuilder(val action: SQLActionBuilder) extends AnyVal {
      def stripMargin: SQLActionBuilder =
        action.copy(action.queryParts.map(_.asInstanceOf[String].stripMargin))
    }

    final case class QueryMetadata(
      sortBy    : Option[String]      = None,
      from      : Option[Int]         = None,
      size      : Option[Int]         = None,
      pageNo    : Option[Int]         = None,
      total     : Option[Future[Int]] = None)

    object QueryMetadata {
      def empty = QueryMetadata()
    }

    final case class ResponseMetadata(
      sortBy    : Option[String] = None,
      from      : Option[Int]    = None,
      size      : Option[Int]    = None,
      pageNo    : Option[Int]    = None,
      total     : Option[Int]    = None)


    final case class ResponseWithMetadata[A](result: Failures Xor A, metadata: ResponseMetadata)

    final case class ResultWithMetadata[A](result: DbResult[A], metadata: QueryMetadata) {

      def wrapExceptions(implicit ec: ExecutionContext): ResultWithMetadata[A] =
        this.copy(result = wrapDbResult(result))

      def map[S](f: A => S)(implicit ec: ExecutionContext): ResultWithMetadata[S] =
        this.copy(result = result.map(_.map(f)))

      def flatMap[S](f: Failures Xor A => DbResult[S])(implicit ec: ExecutionContext): ResultWithMetadata[S] =
        this.copy(result = result.flatMap(f))

      def asResponseFuture(implicit db: Database, ec: ExecutionContext): Future[ResponseWithMetadata[A]] = {
        metadata.total match {
          case None              ⇒
            for (res ← result.run())
              yield ResponseWithMetadata(
                res,
                ResponseMetadata(
                  sortBy = metadata.sortBy,
                  from = metadata.from,
                  size = metadata.size,
                  pageNo = metadata.pageNo,
                  total = None
                )
              )

          case Some(totalFuture) ⇒
            for {
              res   ← result.run()
              total ← totalFuture
            } yield ResponseWithMetadata(
              res,
              ResponseMetadata(
                sortBy = metadata.sortBy,
                from = metadata.from,
                size = metadata.size,
                pageNo = metadata.pageNo,
                total = Some(total)
              )
            )
        }
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

    final case class QueryWithMetadata[E, U, C[_]](query: Query[E, U, C], metadata: QueryMetadata) {

      def sortBy(f: E => Ordered): QueryWithMetadata[E, U, C] =
        this.copy(query = query.sortBy(f))

      def sortIfNeeded(f: (Sort, E) => Ordered)(implicit sortAndPage: SortAndPage): QueryWithMetadata[E, U, C] =
        sortAndPage.sort match {
          case Some(s) ⇒ this.copy(query = query.sortBy(f.curried(s)))
          case None    ⇒ this
        }

      def sortAndPageIfNeeded(f: (Sort, E) => Ordered)
        (implicit sortAndPage: SortAndPage): QueryWithMetadata[E, U, C] = sortIfNeeded(f).paged

      def paged(implicit sortAndPage: SortAndPage): QueryWithMetadata[E, U, C] =
        this.copy(query = _paged(query))

      def result(implicit db: Database, ec: ExecutionContext): ResultWithMetadata[C[U]] =
        ResultWithMetadata(result = DbResult.fromDbio(query.result), metadata)
    }

    implicit class EnrichedQuery[E, U, C[_]](val query: Query[E, U, C]) extends AnyVal {
      def one: DBIO[Option[U]] = query.result.headOption

      def paged(implicit sortAndPage: SortAndPage): Query[E, U, C] = _paged(query)

      def withEmptyMetadata: QueryWithMetadata[E, U, C] = QueryWithMetadata(query, QueryMetadata.empty)

      def withMetadata(metadata: QueryMetadata): QueryWithMetadata[E, U, C] = QueryWithMetadata(query, metadata)

      def withMetadata(implicit db: Database,
        ec: ExecutionContext,
        sortAndPage: SortAndPage): QueryWithMetadata[E, U, C] = {

        val from = sortAndPage.from.getOrElse(0)
        val size = sortAndPage.size.getOrElse(CustomDirectives.DefaultPageSize)

        // size > 0 costraint is defined in SortAndPage
        val pageNo = (from / size) + 1
        // TODO: left as DBIO and compose
        val total  = query.length.result.run()

        val metadata = QueryMetadata(
          sortBy = sortAndPage.sortBy,
          from   = Some(from),
          size   = Some(size),
          pageNo = Some(pageNo),
          total  = Some(total))

        withMetadata(metadata)
      }
    }

    implicit class EnrichedSqlStreamingAction[R, T, E <: Effect](val action: SqlStreamingAction[R, T, E])
      extends AnyVal {

      def one(implicit db: Database, ec: ExecutionContext): Future[Option[T]] =
        db.run(action.headOption)
    }

    implicit class RunOnDbIO[R](val dbio: DBIO[R]) extends AnyVal {
      def run()(implicit db: Database): Future[R] =
        db.run(dbio)

      def toXor(implicit ec: ExecutionContext): DbResult[R] =
        DbResult.fromDbio(dbio)
    }

    implicit class EnrichedDBIOpt[R](val dbio: DBIO[Option[R]]) extends AnyVal {
      def mustFindOr(notFoundFailure: ⇒ Failure)(implicit ec: ExecutionContext): DbResult[R] =
        dbio.flatMap {
          case Some(model) ⇒ DbResult.good(model)
          case None ⇒ DbResult.failure(notFoundFailure)
        }
    }

    implicit class EnrichedDbResult[A](val r: DbResult[A]) extends AnyVal {
      def toXorT(implicit ec: ExecutionContext): DbResultT[A] = DbResultT(r)
    }
  }
}
