package foxcomm.agni.interpreter

import cats.data.NonEmptyList
import cats.implicits._
import foxcomm.agni.dsl.query.QueryFunction
import monix.eval.Coeval
import org.elasticsearch.index.query.{BoolQueryBuilder, QueryBuilders}

package object es {
  type ESQueryInterpreter = Interpreter[NonEmptyList[QueryFunction], Coeval[BoolQueryBuilder]]

  lazy val default: ESQueryInterpreter = ESQueryInterpreter <<< (QueryBuilders.boolQuery() → _)
}
