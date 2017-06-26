package foxcomm.agni.dsl

import cats.data.NonEmptyVector
import foxcomm.agni.dsl.query._
import io.circe.parser._
import org.scalatest.EitherValues._
import org.scalatest.OptionValues._
import org.scalatest.{Assertion, FlatSpec, Matchers}
import scala.io.Source
import shapeless._
import shapeless.syntax.typeable._

class QueryDslSpec extends FlatSpec with Matchers {
  implicit class RichRangeBound[A](val rb: RangeBound[A]) {
    implicit def toMap: Map[RangeFunction, A] = Map.empty ++ rb.lower ++ rb.upper
  }

  def assertQueryFunction[T <: QueryFunction: Typeable](qf: QueryFunction)(
      assertion: T ⇒ Assertion): Assertion =
    assertion(qf.cast[T].value)

  "DSL" should "parse multiple queries" in {
    val json =
      parse(
        Source
          .fromInputStream(getClass.getResourceAsStream("/happy_path.json"))
          .mkString).right.value
    val queries =
      json.as[FCQuery].right.value.query.map(_.toList).getOrElse(Nil)
    assertQueryFunction[QueryFunction.equals](queries.head) { equals ⇒
      equals.field.toList should === (List(Coproduct[Field]("slug")))
      equals.ctx should === (QueryContext.must(Some(Boostable.default)))
      equals.context should be('defined)
      equals.value.toList should === (List("awesome", "whatever"))
    }
    assertQueryFunction[QueryFunction.matches](queries(1)) { matches ⇒
      matches.field.toList should === (
        List(Coproduct[Field]("title"),
             Coproduct[Field]("description"),
             Coproduct[Field](NonEmptyVector.of("skus", "code"))))
      matches.ctx should === (QueryContext.should(Some(0.5f)))
      matches.context should be('defined)
      matches.value.toList should === (List("food", "drink"))
    }
    assertQueryFunction[QueryFunction.range](queries(2)) { range ⇒
      range.field.toList should === (List(Coproduct[Field]("price")))
      range.ctx should === (QueryContext.must(None))
      range.context should be('empty)
      range.value.unify.toMap.mapValues(_.toString) should === (
        Map(
          RangeFunction.Lt  → "5000",
          RangeFunction.Gte → "1000"
        ))
    }
    assertQueryFunction[QueryFunction.exists](queries(3)) { exists ⇒
      exists.value.toList should === (List(Coproduct[Field]("archivedAt")))
      exists.ctx should === (QueryContext.not(None))
      exists.context should be('defined)
    }
  }
}
