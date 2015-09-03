package utils

import scala.concurrent.Future

import slick.ast._
import slick.driver.PostgresDriver._
import slick.driver.PostgresDriver.api._
import slick.jdbc.{GetResult, JdbcResultConverterDomain, SetParameter, StaticQuery ⇒ Q, StaticQueryInvoker, StreamingInvokerAction}
import slick.profile.SqlStreamingAction
import slick.relational.{CompiledMapping, ResultConverter}
import slick.util.SQLBuilder

object Slick {
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
          case Bind(_, _, Pure(Select(_, col), _)) =>
            List(col.name)
          case Bind(_, _, Pure(ProductNode(children), _)) =>
            children.map { case Select(_, col) => col.name }.toList
          case Bind(_, TableExpansion(_, _, TypeMapping(ProductNode(children), _, _)), Pure(Ref(_), _)) =>
            children.map { case Select(_, col) => col.name }.toList
        }

        implicit val pconv: SetParameter[U] = {
          val ResultSetMapping(_, compiled, CompiledMapping(_converter, _)) = updateCompiler.run(updateQuery.toNode).tree
          val converter = _converter.asInstanceOf[ResultConverter[JdbcResultConverterDomain, U]]
          SetParameter[U] { (value, params) =>
            converter.set(value, params.ps)
          }
        }

        implicit val rconv: GetResult[F] = {
          val ResultSetMapping(_, compiled, CompiledMapping(_converter, _)) = queryCompiler.run(returningNode).tree
          val converter = _converter.asInstanceOf[ResultConverter[JdbcResultConverterDomain, F]]
          GetResult[F] { p => converter.read(p.rs) }
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
    implicit class RunOnDbIO[R](val dbio: DBIO[R]) extends AnyVal {
      def run()(implicit db: Database): Future[R] =
        db.run(dbio)
    }
  }
}
