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
import shapeless.Coproduct

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements", "org.wartremover.warts.TraversableOps"))
private[es] object ESQueryInterpreter
    extends QueryInterpreter[Kleisli[Id, ?, BoolQueryBuilder], BoolQueryBuilder] {
  type State = Kleisli[Id, BoolQueryBuilder, BoolQueryBuilder]
  val State: (BoolQueryBuilder ⇒ Id[BoolQueryBuilder]) ⇒ State =
    Kleisli[Id, BoolQueryBuilder, BoolQueryBuilder]

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

  def apply(qfs: NonEmptyList[QueryFunction]): State = State { b ⇒
    qfs.foldM(b)((b, qf) ⇒ eval(qf)(b): Id[BoolQueryBuilder])
  }

  def matchesF(qf: QueryFunction.matches): State = State { b ⇒
    val inContext = b.inContext(qf) _
    for (v ← qf.value.toList) {
      qf.field match {
        case QueryField.Single(n) ⇒ inContext(n.nest(QueryBuilders.matchQuery(_, v)))
        case QueryField.Multiple(ns) ⇒
          val (sfs, nfs) = ns.foldLeft(Vector.empty[String] → Vector.empty[NonEmptyVector[String]]) {
            case ((sAcc, nAcc), f) ⇒
              f.select[String].fold(sAcc)(sAcc :+ _) →
                f.select[NonEmptyVector[String]].fold(nAcc)(nAcc :+ _)
          }
          inContext(QueryBuilders.multiMatchQuery(v, sfs: _*))
          nfs.foreach(nf ⇒ inContext(Coproduct[Field](nf).nest(QueryBuilders.matchQuery(_, v))))
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
          case v :: Nil ⇒ f.nest(QueryBuilders.termQuery(_, v))
          case _        ⇒ f.nest(QueryBuilders.termsQuery(_, vs: _*))
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
        val builder = QueryBuilders.rangeQuery(f)
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

  def rawF(qf: QueryFunction.raw): State = State { b ⇒
    b.inContext(qf)(RawQueryBuilder(qf.value))
  }

  def boolF(qf: QueryFunction.bool): State = State { b ⇒
    qf.value.toNEL.foldM(b)((b, qf) ⇒ eval(qf)(b): Id[BoolQueryBuilder])
  }
}
