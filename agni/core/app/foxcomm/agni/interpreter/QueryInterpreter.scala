package foxcomm.agni.interpreter

import scala.language.higherKinds
import cats.data.NonEmptyList
import foxcomm.agni.dsl.query._

trait QueryInterpreter[F[_], V] extends Interpreter[F, NonEmptyList[QueryFunction], V] {
  final def eval(qf: QueryFunction): F[V] = qf match {
    case qf: QueryFunction.matches ⇒ matchesF(qf)
    case qf: QueryFunction.equals  ⇒ equalsF(qf)
    case qf: QueryFunction.exists  ⇒ existsF(qf)
    case qf: QueryFunction.range   ⇒ rangeF(qf)
    case qf: QueryFunction.raw     ⇒ rawF(qf)
    case qf: QueryFunction.bool    ⇒ boolF(qf)
  }

  def matchesF(qf: QueryFunction.matches): F[V]

  def equalsF(qf: QueryFunction.equals): F[V]

  def existsF(qf: QueryFunction.exists): F[V]

  def rangeF(qf: QueryFunction.range): F[V]

  def rawF(qf: QueryFunction.raw): F[V]

  def boolF(qf: QueryFunction.bool): F[V]
}

object QueryInterpreter {
  @inline implicit def apply[F[_], V](implicit qi: QueryInterpreter[F, V]): QueryInterpreter[F, V] = qi
}
