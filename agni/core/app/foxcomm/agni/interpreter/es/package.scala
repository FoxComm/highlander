package foxcomm.agni.interpreter

import cats.data._
import foxcomm.agni.SearchPayload
import foxcomm.agni.dsl.aggregations._
import foxcomm.agni.dsl.query._
import foxcomm.agni.dsl.sort._
import monix.eval.Coeval
import org.elasticsearch.action.search.{SearchRequest, SearchRequestBuilder}
import org.elasticsearch.index.query.{BoolQueryBuilder, QueryBuilders}
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder
import org.elasticsearch.search.sort.SortBuilder

package object es {
  type ESAggregationInterpreter = Kleisli[Coeval, FCAggregation, List[AbstractAggregationBuilder]]
  type ESQueryInterpreter       = Kleisli[Coeval, FCQuery, BoolQueryBuilder]
  type ESSortInterpreter        = Kleisli[Coeval, FCSort, List[SortBuilder]]
  type ESSearchInterpreter      = Kleisli[Coeval, (SearchPayload, SearchRequestBuilder), SearchRequest]

  val aggregationInterpreter: ESAggregationInterpreter = {
    val eval: Interpreter[Coeval, NonEmptyList[AggregationFunction], List[AbstractAggregationBuilder]] =
      ESAggInterpreter andThen (f ⇒ Coeval.eval(f().toList))
    Kleisli(_.aggs.fold(Coeval.now(List.empty[AbstractAggregationBuilder]))(eval))
  }

  val queryInterpreter: ESQueryInterpreter = {
    val eval: Interpreter[Coeval, NonEmptyList[QueryFunction], BoolQueryBuilder] =
      ESQueryInterpreter andThen (f ⇒ Coeval.eval(f(QueryBuilders.boolQuery())))
    Kleisli(_.query.fold(Coeval.eval(QueryBuilders.boolQuery().must(QueryBuilders.matchAllQuery())))(eval))
  }

  val sortInterpreter: ESSortInterpreter = {
    val eval: Interpreter[Coeval, NonEmptyList[SortFunction], List[SortBuilder]] =
      ESSortInterpreter andThen (f ⇒ Coeval.eval(f().toList))
    Kleisli(_.sorts.fold(Coeval.now(List.empty[SortBuilder]))(eval))
  }

  @SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
  val dslInterpreter: ESSearchInterpreter = Kleisli {
    case (payload, builder) ⇒
      val req = for {
        aggs  ← aggregationInterpreter(payload.aggregations)
        query ← queryInterpreter(payload.query)
        sorts ← sortInterpreter(payload.sort)
      } yield {
        aggs.foreach(builder.addAggregation)
        builder.setQuery(query)
        sorts.foreach(builder.addSort)
        payload.fields.foreach(fs ⇒ builder.setFetchSource(fs.toList.toArray, Array.empty[String]))
        builder.request()
      }
      req
  }
}
