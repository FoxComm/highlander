package foxcomm.agni.interpreter

import cats.data._
import cats.implicits._
import foxcomm.agni.dsl.query.{FCQuery, QueryFunction}
import monix.eval.Coeval
import org.elasticsearch.index.query.{BoolQueryBuilder, QueryBuilders}

package object es {
  type ESQueryInterpreter = Kleisli[Coeval, FCQuery, BoolQueryBuilder]

  lazy val default: ESQueryInterpreter = {
    val eval: Interpreter[Coeval, NonEmptyList[QueryFunction], BoolQueryBuilder] =
      (QueryBuilders.boolQuery() → (_: NonEmptyList[QueryFunction])) >>>
        ESQueryInterpreter

    Kleisli(_.query.fold(Coeval.eval(QueryBuilders.boolQuery().must(QueryBuilders.matchAllQuery())))(eval))
  }
}
