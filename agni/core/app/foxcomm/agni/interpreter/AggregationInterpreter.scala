package foxcomm.agni.interpreter

import scala.language.higherKinds
import cats.data.NonEmptyList
import foxcomm.agni.dsl.aggregations.AggregationFunction

trait AggregationInterpreter[F[_], V] extends Interpreter[F, NonEmptyList[AggregationFunction], V] {
  final def eval(qa: AggregationFunction): F[V] = qa match {
    case qa: AggregationFunction.raw ⇒ rawF(qa)
  }

  def rawF(qf: AggregationFunction.raw): F[V]
}
