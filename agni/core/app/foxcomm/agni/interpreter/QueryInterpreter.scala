package foxcomm.agni.interpreter

import scala.language.higherKinds
import cats.Monad
import cats.data.NonEmptyList
import cats.implicits._
import foxcomm.agni.dsl.query._

abstract class QueryInterpreter[F[_]: Monad, V] extends Interpreter[(V, NonEmptyList[QueryFunction]), F[V]] {
  final def eval(v: V, qf: QueryFunction): F[V] = qf match {
    case qf: QueryFunction.matches ⇒ matchesF(v, qf)
    case qf: QueryFunction.range   ⇒ rangeF(v, qf)
    case qf: QueryFunction.eq      ⇒ eqF(v, qf)
    case qf: QueryFunction.neq     ⇒ neqF(v, qf)
  }

  final def apply(v: (V, NonEmptyList[QueryFunction])): F[V] = v._2.foldM(v._1)(eval)

  def matchesF(v: V, qf: QueryFunction.matches): F[V]

  def rangeF(v: V, qf: QueryFunction.range): F[V]

  def eqF(v: V, qf: QueryFunction.eq): F[V]

  def neqF(v: V, qf: QueryFunction.neq): F[V]
}

object QueryInterpreter {
  @inline implicit def apply[F[_], V](implicit qi: QueryInterpreter[F, V]): QueryInterpreter[F, V] = qi
}
