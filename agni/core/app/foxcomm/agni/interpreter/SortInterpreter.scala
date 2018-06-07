package foxcomm.agni.interpreter

import scala.language.higherKinds
import cats.data.NonEmptyList
import foxcomm.agni.dsl.sort.SortFunction

trait SortInterpreter[F[_], V] extends Interpreter[F, NonEmptyList[SortFunction], V] {
  final def eval(qs: SortFunction): F[V] = qs match {
    case qs: SortFunction.raw ⇒ rawF(qs)
  }

  def rawF(qf: SortFunction.raw): F[V]
}
