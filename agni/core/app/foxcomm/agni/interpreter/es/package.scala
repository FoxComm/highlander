package foxcomm.agni.interpreter

import cats.data.NonEmptyList
import cats.implicits._
import foxcomm.agni.dsl.query.{FCQuery, QueryFunction}
import monix.eval.Coeval
import org.elasticsearch.index.query.{BoolQueryBuilder, QueryBuilders}

package object es {
  type ESQueryInterpreter = Interpreter[FCQuery, Coeval[BoolQueryBuilder]]

  lazy val default: ESQueryInterpreter = {
    val eval: Interpreter[NonEmptyList[QueryFunction], Coeval[BoolQueryBuilder]] =
      ESQueryInterpreter <<< (QueryBuilders.boolQuery() → _)

    _.query.fold(Coeval.eval(QueryBuilders.boolQuery().must(QueryBuilders.matchAllQuery())))(eval)
  }
}
