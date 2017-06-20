package foxcomm.agni.interpreter

import scala.language.higherKinds
import cats.data.NonEmptyList
import cats.implicits._
import cats.{~>, Monad}
import foxcomm.agni._
import foxcomm.agni.dsl.query._
import monix.eval.Coeval
import org.elasticsearch.index.query.{BoolQueryBuilder, QueryBuilders}

sealed abstract class QueryInterpreter[F[_]: Monad, V] extends ((V, NonEmptyList[QueryFunction]) ⇒ F[V]) {
  final def eval(v: V, qf: QueryFunction): F[V] = qf match {
    case qf: QueryFunction.matches ⇒ matchesF(v, qf)
    case qf: QueryFunction.range   ⇒ rangeF(v, qf)
    case qf: QueryFunction.eq      ⇒ eqF(v, qf)
    case qf: QueryFunction.neq     ⇒ neqF(v, qf)
  }

  final def apply(v: V, qfs: NonEmptyList[QueryFunction]): F[V] = qfs.foldM(v)(eval)

  def matchesF(v: V, qf: QueryFunction.matches): F[V]

  def rangeF(v: V, qf: QueryFunction.range): F[V]

  def eqF(v: V, qf: QueryFunction.eq): F[V]

  def neqF(v: V, qf: QueryFunction.neq): F[V]
}

trait LowPriorityQueryInterpreters {
  @inline implicit def defaultQueryInterpreter: QueryInterpreter[Coeval, BoolQueryBuilder] =
    DefaultQueryInterpreter
}

object QueryInterpreter extends LowPriorityQueryInterpreters {
  @inline implicit def apply[F[_], V](implicit qi: QueryInterpreter[F, V]): QueryInterpreter[F, V] = qi

  implicit class RichQueryInterpreter[F1[_], V](private val qi: QueryInterpreter[F1, V]) extends AnyVal {
    def mapTo[F2[_]: Monad](implicit nat: F1 ~> F2): QueryInterpreter[F2, V] = new QueryInterpreter[F2, V] {
      def matchesF(v: V, qf: QueryFunction.matches): F2[V] = nat(qi.matchesF(v, qf))

      def rangeF(v: V, qf: QueryFunction.range): F2[V] = nat(qi.rangeF(v, qf))

      def eqF(v: V, qf: QueryFunction.eq): F2[V] = nat(qi.eqF(v, qf))

      def neqF(v: V, qf: QueryFunction.neq): F2[V] = nat(qi.neqF(v, qf))
    }
  }
}

object DefaultQueryInterpreter extends QueryInterpreter[Coeval, BoolQueryBuilder] {
  def matchesF(b: BoolQueryBuilder, qf: QueryFunction.matches): Coeval[BoolQueryBuilder] = Coeval.evalOnce {
    val fields = qf.in.toList
    qf.value.toList.foldLeft(b)((b, v) ⇒ b.must(QueryBuilders.multiMatchQuery(v, fields: _*)))
  }

  def rangeF(b: BoolQueryBuilder, qf: QueryFunction.range): Coeval[BoolQueryBuilder] = Coeval.evalOnce {
    val builder = QueryBuilders.rangeQuery(qf.in.field)
    val value   = qf.value.unify
    value.lower.foreach {
      case (RangeFunction.Gt, v)  ⇒ builder.gt(v)
      case (RangeFunction.Gte, v) ⇒ builder.gte(v)
    }
    value.upper.foreach {
      case (RangeFunction.Lt, v)  ⇒ builder.lt(v)
      case (RangeFunction.Lte, v) ⇒ builder.lte(v)
    }
    b.filter(builder)
  }

  def eqF(b: BoolQueryBuilder, qf: QueryFunction.eq): Coeval[BoolQueryBuilder] = Coeval.evalOnce {
    val values = qf.value.toList
    qf.in.toList.foldLeft(b)((b, n) ⇒ b.filter(QueryBuilders.termsQuery(n, values: _*)))
  }

  def neqF(b: BoolQueryBuilder, qf: QueryFunction.neq): Coeval[BoolQueryBuilder] = Coeval.evalOnce {
    val values = qf.value.toList
    qf.in.toList.foldLeft(b)((b, n) ⇒ b.mustNot(QueryBuilders.termsQuery(n, values: _*)))
  }
}
