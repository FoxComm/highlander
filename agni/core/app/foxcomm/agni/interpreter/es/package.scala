package foxcomm.agni.interpreter

import cats.data._
import foxcomm.agni.dsl.query.{FCQuery, QueryFunction}
import foxcomm.agni.dsl.sort.{FCSort, SortFunction}
import monix.eval.Coeval
import org.elasticsearch.index.query.{BoolQueryBuilder, QueryBuilders}
import org.elasticsearch.search.sort.SortBuilder

package object es {
  type ESQueryInterpreter = Kleisli[Coeval, FCQuery, BoolQueryBuilder]
  type ESSortInterpreter  = Kleisli[Coeval, FCSort, List[SortBuilder]]

  val queryInterpreter: ESQueryInterpreter = {
    val eval: Interpreter[Coeval, NonEmptyList[QueryFunction], BoolQueryBuilder] =
      ESQueryInterpreter andThen (f ⇒ Coeval.eval(f(QueryBuilders.boolQuery())))
    Kleisli(_.query.fold(Coeval.eval(QueryBuilders.boolQuery().must(QueryBuilders.matchAllQuery())))(eval))
  }

  val sortInterpreter: ESSortInterpreter = {
    val eval: Interpreter[Coeval, NonEmptyList[SortFunction], List[SortBuilder]] =
      ESSortInterpreter andThen (f ⇒ Coeval.eval(f().toList))
    Kleisli(_.sort.fold(Coeval.now(List.empty[SortBuilder]))(eval))
  }
}
