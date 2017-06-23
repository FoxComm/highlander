package foxcomm.agni.interpreter.es

import cats.data.{NonEmptyList, NonEmptyVector}
import cats.implicits._
import foxcomm.agni._
import foxcomm.agni.dsl.query._
import foxcomm.agni.interpreter.QueryInterpreter
import io.circe.JsonObject
import monix.cats._
import monix.eval.Coeval
import org.elasticsearch.common.xcontent.{ToXContent, XContentBuilder}
import org.elasticsearch.index.query._
import shapeless.Coproduct

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements", "org.wartremover.warts.TraversableOps"))
object ESQueryInterpreter extends QueryInterpreter[Coeval, BoolQueryBuilder] {
  private implicit class RichBoolQueryBuilder(val b: BoolQueryBuilder) extends AnyVal {
    def inContext(qf: QueryFunction.WithContext)(qb: QueryBuilder): BoolQueryBuilder = qf.ctx match {
      case QueryContext.filter ⇒ b.filter(qb)
      case QueryContext.must   ⇒ b.must(qb)
      case QueryContext.should ⇒ b.should(qb)
      case QueryContext.not    ⇒ b.mustNot(qb)
    }
  }

  private implicit class RichField(val f: Field) extends AnyVal {
    def nest(q: String ⇒ QueryBuilder): QueryBuilder =
      f.eliminate(q,
                  _.eliminate(fs ⇒ fs.toVector.init.foldRight(q(fs.toVector.last))(QueryBuilders.nestedQuery),
                              _.impossible))
  }

  private final case class RawQueryBuilder(content: JsonObject) extends QueryBuilder {
    def doXContent(builder: XContentBuilder, params: ToXContent.Params): Unit = {
      builder.startObject()
      content.toMap.foreach {
        case (n, v) ⇒
          builder.rawField(n, v.dump)
      }
      builder.endObject()
    }
  }

  def apply(v: (BoolQueryBuilder, NonEmptyList[QueryFunction])): Coeval[BoolQueryBuilder] =
    v._2.foldM(v._1)(eval)

  def matchesF(b: BoolQueryBuilder, qf: QueryFunction.matches): Coeval[BoolQueryBuilder] =
    Coeval.eval {
      qf.value.toNEL.foldLeft(b)((b, v) ⇒
        qf.field match {
          case QueryField.Single(n) ⇒ b.inContext(qf)(n.nest(QueryBuilders.matchQuery(_, v)))
          case QueryField.Multiple(ns) ⇒
            val (s, n) = ns.foldLeft(Vector.empty[String] → Vector.empty[NonEmptyVector[String]]) {
              case ((sAcc, nAcc), f) ⇒
                f.select[String].fold(sAcc)(sAcc :+ _) →
                  f.select[NonEmptyVector[String]].fold(nAcc)(nAcc :+ _)
            }
            n.foldLeft(b.inContext(qf)(QueryBuilders.multiMatchQuery(v, s: _*)))(
              (acc, f) ⇒ acc.inContext(qf)(Coproduct[Field](f).nest(QueryBuilders.matchQuery(_, v)))
            )
      })
    }

  def equalsF(b: BoolQueryBuilder, qf: QueryFunction.equals): Coeval[BoolQueryBuilder] =
    Coeval.eval {
      val vs = qf.value.toNEL.toList
      qf.in.toNEL.foldLeft(b)((b, n) ⇒
        b.inContext(qf) {
          vs match {
            case v :: Nil ⇒ n.nest(QueryBuilders.termQuery(_, v))
            case _        ⇒ n.nest(QueryBuilders.termsQuery(_, vs: _*))
          }
      })
    }

  def existsF(b: BoolQueryBuilder, qf: QueryFunction.exists): Coeval[BoolQueryBuilder] =
    Coeval.eval {
      qf.value.toNEL.foldLeft(b)((b, n) ⇒ b.inContext(qf)(n.nest(QueryBuilders.existsQuery)))
    }

  def rangeF(b: BoolQueryBuilder, qf: QueryFunction.range): Coeval[BoolQueryBuilder] = Coeval.eval {
    b.inContext(qf) {
      qf.in.field.nest { n ⇒
        val builder = QueryBuilders.rangeQuery(n)
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
  }

  def rawF(b: BoolQueryBuilder, qf: QueryFunction.raw): Coeval[BoolQueryBuilder] = Coeval.eval {
    b.inContext(qf)(RawQueryBuilder(qf.value))
  }

  def boolF(b: BoolQueryBuilder, qf: QueryFunction.bool): Coeval[BoolQueryBuilder] =
    qf.value.toNEL.foldM(b)(eval)
}
