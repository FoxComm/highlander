package foxcomm.agni.interpreter.es

import foxcomm.agni.dsl.query._
import foxcomm.agni.interpreter.QueryInterpreter
import monix.cats._
import monix.eval.Coeval
import org.elasticsearch.index.query._

@SuppressWarnings(Array("org.wartremover.warts.Overloading"))
object ESQueryInterpreter extends QueryInterpreter[Coeval, BoolQueryBuilder] {
  implicit class RichBoolQueryBuilder(val b: BoolQueryBuilder) extends AnyVal {
    def inContext(qf: QueryFunction.WithContext)(qb: ⇒ QueryBuilder): BoolQueryBuilder = qf.context match {
      case QueryContext.filter ⇒ b.filter(qb)
      case QueryContext.must   ⇒ b.must(qb)
      case QueryContext.should ⇒ b.should(qb)
      case QueryContext.not    ⇒ b.mustNot(qb)
    }

    def foreachField(qf: QueryFunction.WithField)(
        f: (BoolQueryBuilder, String) ⇒ BoolQueryBuilder): BoolQueryBuilder = {
      qf.in.toList.foreach(f(b, _))
      b
    }

    def foreachField(qf: QueryFunction.WithContext with QueryFunction.WithField)(
        f: String ⇒ QueryBuilder): BoolQueryBuilder = {
      qf.in.toList.foreach(n ⇒ b.inContext(qf)(f(n)))
      b
    }
  }

  def matchesF(b: BoolQueryBuilder, qf: QueryFunction.matches): Coeval[BoolQueryBuilder] = Coeval.eval {
    val fields = qf.in.toList
    qf.value.toList.foldLeft(b)((b, v) ⇒
      b.inContext(qf) {
        QueryBuilders.multiMatchQuery(v, fields: _*)
    })
  }

  def rangeF(b: BoolQueryBuilder, qf: QueryFunction.range): Coeval[BoolQueryBuilder] = Coeval.eval {
    b.inContext(qf) {
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
      builder
    }
  }

  def eqF(b: BoolQueryBuilder, qf: QueryFunction.eq): Coeval[BoolQueryBuilder] = Coeval.eval {
    val values = qf.value.toList
    b.foreachField(qf) { n ⇒
      QueryBuilders.termsQuery(n, values: _*)
    }
  }

  def neqF(b: BoolQueryBuilder, qf: QueryFunction.neq): Coeval[BoolQueryBuilder] = Coeval.eval {
    val values = qf.value.toList
    b.foreachField(qf) { n ⇒
      QueryBuilders.termsQuery(n, values: _*)
    }
  }
}
