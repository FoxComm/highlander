package foxcomm.agni.interpreter

import cats.data._
import foxcomm.agni.dsl.query.{FCQuery, QueryFunction}
import monix.eval.Coeval
import org.elasticsearch.index.query.{BoolQueryBuilder, QueryBuilders}

package object es {
  type ESQueryInterpreter = Kleisli[Coeval, FCQuery, BoolQueryBuilder]

  val queryInterpreter: ESQueryInterpreter = {
    val eval: Interpreter[Coeval, NonEmptyList[QueryFunction], BoolQueryBuilder] =
      ESQueryInterpreter andThen (f ⇒ Coeval.eval(f(QueryBuilders.boolQuery())))
    Kleisli(_.query.fold(Coeval.eval(QueryBuilders.boolQuery().must(QueryBuilders.matchAllQuery())))(eval))
  }
}
