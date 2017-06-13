package foxcomm.search.dsl

import foxcomm.search.dsl.query._
import io.circe.parser._
import org.scalatest.EitherValues._
import org.scalatest.OptionValues._
import org.scalatest.{Assertion, FlatSpec, Matchers}
import scala.io.Source
import shapeless._
import shapeless.syntax.typeable._

class QueryDslSpec extends FlatSpec with Matchers {
  def assertQueryFunction[T <: QueryFunction: Typeable](qf: QueryFunction)(
      assertion: T ⇒ Assertion): Assertion =
    assertion(qf.cast[T].value)

  "DSL" should "parse multiple queries" in {
    val json =
      parse(Source.fromInputStream(getClass.getResourceAsStream("/happy_path.json")).mkString).right.value
    val queries = json.as[FCQuery].right.value.query.toList
    assertQueryFunction[QueryFunction.eq](queries.head) { is ⇒
      is.in.toList should === (List("slug"))
      is.value.toList should === (List("awesome", "whatever"))
    }
    assertQueryFunction[QueryFunction.state](queries(1)) { state ⇒
      state.value should === (EntityState.all)
    }
    assertQueryFunction[QueryFunction.matches](queries(2)) { matches ⇒
      matches.in.toList should === (List("title", "description"))
      matches.value.toList should === (List("food", "drink"))
    }
    assertQueryFunction[QueryFunction.range](queries(3)) { range ⇒
      range.in.toList should === (List("price"))
      range.value.unify.toMap.mapValues(_.toString) should === (
        Map(
          RangeFunction.Lt  → "5000",
          RangeFunction.Gte → "1000"
        ))
    }
  }
}
