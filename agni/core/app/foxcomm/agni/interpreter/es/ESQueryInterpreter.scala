package foxcomm.agni.interpreter.es

import cats.Id
import cats.data._
import cats.implicits._
import foxcomm.agni._
import foxcomm.agni.dsl.query._
import foxcomm.agni.interpreter.QueryInterpreter
import io.circe.JsonObject
import org.elasticsearch.common.xcontent.{ToXContent, XContentBuilder}
import org.elasticsearch.index.query._
import scala.annotation.tailrec

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements", "org.wartremover.warts.TraversableOps"))
private[es] object ESQueryInterpreter
    extends QueryInterpreter[Kleisli[Id, ?, BoolQueryBuilder], BoolQueryBuilder] {
  type State = Kleisli[Id, BoolQueryBuilder, BoolQueryBuilder]
  val State: (BoolQueryBuilder ⇒ Id[BoolQueryBuilder]) ⇒ State =
    Kleisli[Id, BoolQueryBuilder, BoolQueryBuilder]

  private def buildNestedQuery(path: NonEmptyVector[String])(q: String ⇒ QueryBuilder): QueryBuilder = {
    @tailrec def rec(path: Vector[String], acc: QueryBuilder): QueryBuilder = path match {
      case Vector() :+ l ⇒
        QueryBuilders.nestedQuery(l, acc)
      case i :+ l ⇒
        rec(i, QueryBuilders.nestedQuery(l, acc))
    }

    val combined = path.tail.iterator.scanLeft(path.head)((p, f) ⇒ s"$p.$f").toVector
    rec(combined.init, q(combined.last))
  }

  private implicit class RichBoolQueryBuilder(val b: BoolQueryBuilder) extends AnyVal {
    def inContext(qf: QueryFunction.WithContext)(qb: QueryBuilder): BoolQueryBuilder = qf.ctx match {
      case QueryContext.filter ⇒ b.filter(qb)
      case QueryContext.must   ⇒ b.must(qb)
      case QueryContext.should ⇒ b.should(qb)
      case QueryContext.not    ⇒ b.mustNot(qb)
    }
  }

  private implicit class RichBoostableQueryBuilder[B <: QueryBuilder with BoostableQueryBuilder[B]](val b: B)
      extends AnyVal {
    def withBoost(qf: QueryFunction.WithField): B = qf.boost.fold(b)(b.boost)
  }

  private implicit class RichField(val f: Field) extends AnyVal {
    def nest(q: String ⇒ QueryBuilder): QueryBuilder =
      f.eliminate(
        q,
        _.eliminate(
          buildNestedQuery(_)(q),
          _.impossible
        )
      )
  }

  private final case class RawQueryBuilder(content: JsonObject) extends QueryBuilder {

    def doXContent(builder: XContentBuilder, params: ToXContent.Params): Unit =
      content.toMap.foreach {
        case (n, v) ⇒
          builder.rawField(n, v.toSmile)
      }
  }

  def apply(qfs: NonEmptyList[QueryFunction]): State = State { b ⇒
    qfs.foldM(b)((b, qf) ⇒ eval(qf)(b): Id[BoolQueryBuilder])
  }

  def matchesF(qf: QueryFunction.matches): State = State { b ⇒
    val inContext = b.inContext(qf) _
    for (v ← qf.value.toList) {
      qf.field match {
        case QueryField.Single(n) ⇒ inContext(n.nest(QueryBuilders.matchQuery(_, v).withBoost(qf)))
        case QueryField.Multiple(ns) ⇒
          val (sfs, nfs) = ns.foldLeft(Vector.empty[String] → Vector.empty[NonEmptyVector[String]]) {
            case ((sAcc, nAcc), f) ⇒
              f.select[String].fold(sAcc)(sAcc :+ _) →
                f.select[NonEmptyVector[String]].fold(nAcc)(nAcc :+ _)
          }
          if (sfs.nonEmpty) inContext(QueryBuilders.multiMatchQuery(v, sfs: _*).withBoost(qf))
          nfs.foreach(nf ⇒ inContext(buildNestedQuery(nf)(QueryBuilders.matchQuery(_, v).withBoost(qf))))
      }
    }
    b
  }

  def equalsF(qf: QueryFunction.equals): State = State { b ⇒
    val inContext = b.inContext(qf) _
    val vs        = qf.value.toList
    for (f ← qf.in.toList) {
      inContext {
        vs match {
          case v :: Nil ⇒ f.nest(QueryBuilders.termQuery(_, v).withBoost(qf))
          case _        ⇒ f.nest(QueryBuilders.termsQuery(_, vs: _*).withBoost(qf))
        }
      }
    }
    b
  }

  def existsF(qf: QueryFunction.exists): State = State { b ⇒
    val inContext = b.inContext(qf) _
    qf.value.toList.foreach(f ⇒ inContext(f.nest(QueryBuilders.existsQuery)))
    b
  }

  def rangeF(qf: QueryFunction.range): State = State { b ⇒
    b.inContext(qf) {
      qf.in.field.nest { f ⇒
        val builder = QueryBuilders.rangeQuery(f).withBoost(qf)
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
    b
  }

  def rawF(qf: QueryFunction.raw): State = State { b ⇒
    b.inContext(qf)(RawQueryBuilder(qf.value))
    b
  }

  def boolF(qf: QueryFunction.bool): State = State { b ⇒
    b.inContext(qf)(apply(qf.value.toNEL)(QueryBuilders.boolQuery()))
    b
  }
}
