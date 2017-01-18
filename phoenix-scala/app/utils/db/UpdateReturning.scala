package utils.db

import scala.util.matching.Regex

import cats.data.Xor
import failures.Failure
import slick.ast.{CompiledStatement, ResultSetMapping}
import slick.dbio.Effect
import slick.driver.PostgresDriver._
import slick.jdbc.{GetResult, JdbcResultConverterDomain, SQLActionBuilder, SetParameter}
import slick.lifted.Query
import slick.profile.SqlStreamingAction
import slick.relational.{CompiledMapping, ResultConverter}
import slick.util.SQLBuilder
import utils.aliases._

/*
 * Provides an implicit conversion to allow for UDPATE _ RETURNING _ queries
 * Usage: Customers.filter(_.id === 1).map(_.firstName).
 *  updateReturning(Customers.map(_.firstName), ("blah"))
 *
 * This was generously copied from and upgraded to Slick 3.0 from: http://stackoverflow.com/a/28148606/310275
 */
object UpdateReturning {
  val columnRegex: Regex = "(\".*\")".r

  implicit class UpdateReturningInvoker[E, U, C[_]](val updateQuery: Query[E, U, C])
      extends AnyVal {

    def updateReturningHead[A, F](returningQuery: Query[A, F, C], v: U)(
        implicit ec: EC): DbResultT[F] =
      ExceptionWrapper.wrapDbio(updateReturning(returningQuery, v).head)

    def updateReturningHeadOption[A, F](
        returningQuery: Query[A, F, C],
        v: U,
        notFoundFailure: Failure)(implicit ec: EC): DbResultT[F] = {
      val returningResult = updateReturning(returningQuery, v)
      val withFailure = returningResult.headOption.dbresult.flatMap(res ⇒
        DbResultT.fromXor(Xor.fromOption(res, notFoundFailure.single)))
      ExceptionWrapper.wrapDbResultT(withFailure)
    }

    private def updateReturning[A, F](returningQuery: Query[A, F, C],
                                      v: U): SqlStreamingAction[Vector[F], F, Effect.All] = {
      val ResultSetMapping(_,
                           CompiledStatement(_, sres: SQLBuilder.Result, _),
                           CompiledMapping(_updateConverter, _)) =
        updateCompiler.run(updateQuery.toNode).tree

      val pconv: SetParameter[U] = {
        val ResultSetMapping(_, compiled, CompiledMapping(_converter, _)) =
          updateCompiler.run(updateQuery.toNode).tree
        val converter = _converter.asInstanceOf[ResultConverter[JdbcResultConverterDomain, U]]
        SetParameter[U] { (value, params) ⇒
          converter.set(value, params.ps)
        }
      }

      // extract the result/converter to build our RETURNING {columns} and re-use it for result conversion
      val ResultSetMapping(_,
                           CompiledStatement(_, returningResult: SQLBuilder.Result, _),
                           CompiledMapping(resultConverter, _)) =
        queryCompiler.run(returningQuery.toNode).tree

      val rconv: GetResult[F] = {
        val converter = resultConverter.asInstanceOf[ResultConverter[JdbcResultConverterDomain, F]]
        GetResult[F] { p ⇒
          converter.read(p.rs)
        }
      }

      // extract columns from the `SELECT {columns} FROM {table}` after dropping `FROM .*` from query str
      val columns      = columnRegex.findAllIn(returningResult.sql.replaceAll(" from .*", "")).toList
      val fieldsExp    = columns.mkString(", ")
      val returningSql = sres.sql + s" RETURNING $fieldsExp"

      SQLActionBuilder(returningSql, pconv.applied(v)).as[F](rconv)
    }
  }
}
