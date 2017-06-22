package foxcomm.agni.interpreter.es

import foxcomm.agni._
import foxcomm.agni.dsl.query._
import foxcomm.agni.interpreter.QueryInterpreter
import io.circe.JsonObject
import monix.cats._
import monix.eval.Coeval
import org.elasticsearch.common.xcontent.{ToXContent, XContentBuilder}
import org.elasticsearch.index.query._

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
object ESQueryInterpreter extends QueryInterpreter[Coeval, BoolQueryBuilder] {
  implicit class RichBoolQueryBuilder(val b: BoolQueryBuilder) extends AnyVal {
    def inContext(qf: QueryFunction.WithContext)(qb: ⇒ QueryBuilder): BoolQueryBuilder = qf.ctx match {
      case QueryContext.filter ⇒ b.filter(qb)
      case QueryContext.must   ⇒ b.must(qb)
      case QueryContext.should ⇒ b.should(qb)
      case QueryContext.not    ⇒ b.mustNot(qb)
    }
  }

  final case class RawQueryBuilder(content: JsonObject) extends QueryBuilder {
    def doXContent(builder: XContentBuilder, params: ToXContent.Params): Unit = {
      builder.startObject()
      content.toMap.foreach {
        case (n, v) ⇒
          builder.rawField(n, v.dump)
      }
      builder.endObject()
    }
  }

  def matchesF(b: BoolQueryBuilder, qf: QueryFunction.matches): Coeval[BoolQueryBuilder] = Coeval.eval {
    qf.value.toList.foldLeft(b)((b, v) ⇒
      b.inContext(qf) {
        qf.field match {
          case QueryField.Single(n)    ⇒ QueryBuilders.matchQuery(n, v)
          case QueryField.Multiple(ns) ⇒ QueryBuilders.multiMatchQuery(v, ns.toList: _*)
        }
    })
  }

  def equalsF(b: BoolQueryBuilder, qf: QueryFunction.equals): Coeval[BoolQueryBuilder] = Coeval.eval {
    val vs = qf.value.toList
    qf.in.toList.foldLeft(b)((b, n) ⇒
      b.inContext(qf) {
        vs match {
          case v :: Nil ⇒ QueryBuilders.termQuery(n, v)
          case _        ⇒ QueryBuilders.termsQuery(n, vs: _*)
        }
    })
  }

  def existsF(b: BoolQueryBuilder, qf: QueryFunction.exists): Coeval[BoolQueryBuilder] = Coeval.eval {
    qf.value.toList.foldLeft(b)((b, n) ⇒
      b.inContext(qf) {
        QueryBuilders.existsQuery(n)
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

  def rawF(b: BoolQueryBuilder, qf: QueryFunction.raw): Coeval[BoolQueryBuilder] = Coeval.eval {
    b.inContext(qf)(RawQueryBuilder(qf.value))
  }
}
