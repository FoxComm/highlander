package foxcomm.agni.dsl

import cats.data.NonEmptyVector
import foxcomm.agni.dsl.query._
import io.circe.{Json, JsonObject}
import io.circe.parser._
import org.scalatest.EitherValues._
import org.scalatest.OptionValues._
import org.scalatest.{Assertion, FlatSpec, Matchers}
import scala.annotation.tailrec
import scala.io.Source
import shapeless._
import shapeless.syntax.typeable._

class QueryDslSpec extends FlatSpec with Matchers {
  implicit class RichRangeBound[A](val rb: RangeBound[A]) {
    implicit def toMap: Map[RangeFunction, A] = Map.empty ++ rb.lower ++ rb.upper
  }

  def assertQueryFunction[T <: QueryFunction: Typeable](qf: QueryFunction)(
      assertion: T ⇒ Assertion): Assertion =
    qf.cast[T]
      .fold(fail(s"Cannot cast query function ${qf.getClass.getName} to ${Typeable[T].describe}"))(assertion)

  "DSL" should "parse multiple queries" in {
    val json =
      parse(
        Source
          .fromInputStream(getClass.getResourceAsStream("/query/multiple.json"))
          .mkString).right.value
    val queries =
      json.as[FCQuery].right.value.query.map(_.toList).getOrElse(Nil)
    assertQueryFunction[QueryFunction.equals](queries.head) { equals ⇒
      equals.field.toList should === (List(Coproduct[Field]("slug")))
      equals.ctx should === (QueryContext.must)
      equals.context should be('defined)
      equals.value.toList should === (List("awesome", "whatever"))
    }
    assertQueryFunction[QueryFunction.matches](queries(1)) { matches ⇒
      matches.field.toList should === (
        List(Coproduct[Field]("title"),
             Coproduct[Field]("description"),
             Coproduct[Field](NonEmptyVector.of("skus", "code"))))
      matches.ctx should === (QueryContext.should)
      matches.boost.value should === (0.5f)
      matches.context should be('defined)
      matches.value.toList should === (List("food", "drink"))
    }
    assertQueryFunction[QueryFunction.range](queries(2)) { range ⇒
      range.field.toList should === (List(Coproduct[Field]("price")))
      range.ctx should === (QueryContext.filter)
      range.context should be('empty)
      range.value.unify.toMap.mapValues(_.toString) should === (
        Map(
          RangeFunction.Lt  → "5000",
          RangeFunction.Gte → "1000"
        ))
    }
    assertQueryFunction[QueryFunction.exists](queries(3)) { exists ⇒
      exists.value.toList should === (List(Coproduct[Field]("archivedAt")))
      exists.ctx should === (QueryContext.not)
      exists.context should be('defined)
    }
    assertQueryFunction[QueryFunction.raw](queries(4)) { raw ⇒
      raw.context should === (QueryContext.filter)
      raw.value should === (JsonObject.singleton("match_all", Json.fromJsonObject(JsonObject.empty)))
    }
    assertQueryFunction[QueryFunction.bool](queries(5)) { bool ⇒
      bool.context should === (QueryContext.should)
      val qfs = bool.value.toNEL
      assertQueryFunction[QueryFunction.equals](qfs.head) { equals ⇒
        equals.field.toList should === (List(Coproduct[Field]("context")))
        equals.value.toList should === (List("default"))
      }
      assertQueryFunction[QueryFunction.bool](qfs.tail.head) { bool ⇒
        bool.context should === (QueryContext.not)
        assertQueryFunction[QueryFunction.exists](bool.value.toNEL.head) { exists ⇒
          exists.value.toList should === (List(Coproduct[Field]("context")))
        }
      }
    }
  }

  it should "limit max depth for bool query" in {
    val leaf = JsonObject.fromMap(
      Map(
        "type"  → Json.fromString("exists"),
        "value" → Json.arr(Json.fromString("answer"), Json.fromString("to"), Json.fromString("everything"))
      ))

    @tailrec
    def deepBool(boolDepth: Int, embed: JsonObject): Json =
      if (boolDepth > 0)
        deepBool(boolDepth - 1,
                 JsonObject.fromMap(
                   Map(
                     "type"    → Json.fromString("bool"),
                     "context" → Json.fromString("filter"),
                     "value"   → Json.fromJsonObject(embed)
                   )))
      else Json.fromJsonObject(embed)

    deepBool(boolDepth = QueryFunction.bool.MaxDepth - 1, embed = leaf).as[FCQuery].isLeft should === (false)
    deepBool(boolDepth = QueryFunction.bool.MaxDepth, embed = leaf).as[FCQuery].isLeft should === (true)
  }
}
