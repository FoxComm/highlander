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
      equals.in.toList should === (List(Coproduct[Field]("slug")))
      equals.value.toList should === (List("awesome", "whatever"))
    }
    assertQueryFunction[QueryFunction.matches](queries(1)) { matches ⇒
      matches.field.toList should === (
        List(Coproduct[Field]("title"),
             Coproduct[Field]("description"),
             Coproduct[Field](NonEmptyVector.of("skus", "code"))))
      matches.value.toList should === (List("food", "drink"))
    }
    assertQueryFunction[QueryFunction.range](queries(2)) { range ⇒
      range.in.toList should === (List(Coproduct[Field]("price")))
      range.value.unify.toMap.mapValues(_.toString) should === (
        Map(
          RangeFunction.Lt  → "5000",
          RangeFunction.Gte → "1000"
        ))
    }
    assertQueryFunction[QueryFunction.exists](queries(3)) { exists ⇒
      exists.value.toList should === (List(Coproduct[Field]("archivedAt")))
      exists.ctx should === (QueryContext.not)
    }
  }
}
