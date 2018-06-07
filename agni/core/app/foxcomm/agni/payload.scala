package foxcomm.agni

import cats.data.NonEmptyList
import foxcomm.agni.dsl.aggregations.FCAggregation
import foxcomm.agni.dsl.query.FCQuery
import foxcomm.agni.dsl.sort.FCSort

final case class SearchPayload(aggregations: FCAggregation,
                               query: FCQuery,
                               sort: FCSort,
                               fields: Option[NonEmptyList[String]])
