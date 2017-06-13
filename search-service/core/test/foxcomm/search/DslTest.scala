package foxcomm.search

import io.circe.JsonNumber
import io.circe.generic.extras.auto._
import io.circe.parser._
import org.scalatest.EitherValues._
import org.scalatest.OptionValues._
import org.scalatest.{Assertion, FlatSpec, Matchers}
import scala.io.Source
import shapeless._
import shapeless.syntax.typeable._

class DslTest extends FlatSpec with Matchers {
  def assertQueryFunction[T <: QueryFunction: Typeable](qf: QueryFunction)(
      assertion: T ⇒ Assertion): Assertion =
    assertion(qf.cast[T].value)

  "DSL" should "parse multiple queries" in {
    val json =
      parse(Source.fromInputStream(getClass.getResourceAsStream("/happy_path.json")).mkString).right.value
    val queries = json.as[SearchQuery.fc].right.value.query.query.toList
    assertQueryFunction[QueryFunction.is](queries.head) { is ⇒
      is.in.toList should === (List("slug"))
      is.value.fold(QueryFunction.listOfAnyValueF) should === (List("awesome", "whatever"))
    }
    assertQueryFunction[QueryFunction.state](queries(1)) { state ⇒
      state.value should === (EntityState.all)
    }
    assertQueryFunction[QueryFunction.contains](queries(2)) { contains ⇒
      contains.in.toList should === (List("tags"))
      contains.value.fold(QueryFunction.listOfAnyValueF) should === (List("gift"))
    }
    assertQueryFunction[QueryFunction.matches](queries(3)) { matches ⇒
      matches.in.toList should === (List("title", "description"))
      matches.value.fold(QueryFunction.listOfAnyValueF) should === (List("food", "drink"))
    }
    assertQueryFunction[QueryFunction.range](queries(4)) { range ⇒
      range.in.toList should === (List("price"))
      range.value.unify.cast[Map[RangeFunction, JsonNumber]].value.mapValues(_.toString) should === (
        Map(
          RangeFunction.Lt  → "5000",
          RangeFunction.Gte → "1000"
        ))
    }
  }
}
