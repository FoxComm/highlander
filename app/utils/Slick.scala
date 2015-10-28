package utils

import scala.concurrent.{ExecutionContext, Future}

import cats.data.Xor
import models.Orders
import responses.FullOrder
import services.{Failure, Failures, Result}
import slick.ast._
import slick.driver.PostgresDriver._
import slick.driver.PostgresDriver.api._
import slick.jdbc.{GetResult, JdbcResultConverterDomain, SetParameter, StaticQuery ⇒ Q, StaticQueryInvoker, StreamingInvokerAction}

import slick.lifted.{Ordered, Query}
import slick.profile.{SqlAction, SqlStreamingAction}
import slick.relational.{CompiledMapping, ResultConverter}
import slick.util.SQLBuilder
import utils.CustomDirectives.{Sort, SortAndPage}

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
    implicit class UpdateReturningInvoker[E, U, C[_]](val updateQuery: Query[E, U, C]) extends AnyVal {

      @SuppressWarnings(Array("org.brianmckenna.wartremover.warts.Any",
        "org.brianmckenna.wartremover.warts.IsInstanceOf", "org.brianmckenna.wartremover.warts.AsInstanceOf"))
      def updateReturning[A, F](returningQuery: Query[A, F, C], v: U)(implicit db: Database) = {
        val ResultSetMapping(_,
          CompiledStatement(_, sres: SQLBuilder.Result, _),
          CompiledMapping(_updateConverter, _)) = updateCompiler.run(updateQuery.toNode).tree

        val returningNode = returningQuery.toNode
        val fieldNames = returningNode match {
          case Bind(_, _, Pure(Select(_, col), _)) ⇒
            List(col.name)
          case Bind(_, _, Pure(ProductNode(children), _)) ⇒
            children.map { case Select(_, col) ⇒ col.name }.toList
          case Bind(_, TableExpansion(_, _, TypeMapping(ProductNode(children), _, _)), Pure(Ref(_), _)) ⇒
            children.map { case Select(_, col) ⇒ col.name }.toList
        }

        implicit val pconv: SetParameter[U] = {
          val ResultSetMapping(_, compiled, CompiledMapping(_converter, _)) = updateCompiler.run(updateQuery.toNode).tree
          val converter = _converter.asInstanceOf[ResultConverter[JdbcResultConverterDomain, U]]
          SetParameter[U] { (value, params) ⇒
            converter.set(value, params.ps)
          }
        }

        implicit val rconv: GetResult[F] = {
          val ResultSetMapping(_, compiled, CompiledMapping(_converter, _)) = queryCompiler.run(returningNode).tree
          val converter = _converter.asInstanceOf[ResultConverter[JdbcResultConverterDomain, F]]
          GetResult[F] { p ⇒ converter.read(p.rs) }
        }

        val fieldsExp = fieldNames.map(quoteIdentifier).mkString(", ")
        val pconvUnit = pconv.applied(v)
        val sql = sres.sql + s" RETURNING ${fieldsExp}"
        val unboundQuery = Q.query[U, F](sql)
        val boundQuery = unboundQuery(v)

        new StreamingInvokerAction[Vector[F], F, Effect] {
          def statements = List(boundQuery.getStatement)
          protected[this] def createInvoker(statements: Iterable[String]) = new StaticQueryInvoker[Unit, F](statements.head, pconvUnit, (), rconv)
          protected[this] def createBuilder = Vector.newBuilder[F]
        }.asInstanceOf[SqlStreamingAction[Vector[F], F, Effect]]
      }
    }
  }

  object implicits {
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

      def map[S](f: A => S)(implicit ec: ExecutionContext): ResultWithMetadata[S] =
        this.copy(result = result.map(_.map(f)))

      def flatMap[S](f: Failures Xor A => DbResult[S])(implicit ec: ExecutionContext): ResultWithMetadata[S] =
        this.copy(result = result.flatMap(f))

      def asResponseFuture(implicit db: Database, ec: ExecutionContext): Future[ResponseWithMetadata[A]] = {
        metadata.total match {
          case None              ⇒
            for (res ← result.transactionally.run())
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
              res   ← result.transactionally.run()
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

        // size > 0 costraint is defined in SortAndPage
        val total = sortAndPage.size map { _ ⇒ query.length.result.run() }
        val pageNo = for {
          from ← sortAndPage.from
          size ← sortAndPage.size
        } yield (from / size) + 1

        val metadata = QueryMetadata(
          sortBy = sortAndPage.sortBy,
          from = sortAndPage.from,
          size = sortAndPage.size,
          pageNo = pageNo,
          total = total)

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
    }
  }
}
