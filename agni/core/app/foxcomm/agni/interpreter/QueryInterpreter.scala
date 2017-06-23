package foxcomm.agni.interpreter

import scala.language.higherKinds
import cats.data.{Kleisli, NonEmptyList}
import foxcomm.agni.dsl.query._

trait QueryInterpreter[F[_], V] extends Interpreter[F, (V, NonEmptyList[QueryFunction]), V] {
  final def kleisli: Kleisli[F, (V, NonEmptyList[QueryFunction]), V] = Kleisli(this)

  final def eval(v: V, qf: QueryFunction): F[V] = qf match {
    case qf: QueryFunction.matches ⇒ matchesF(v, qf)
    case qf: QueryFunction.equals  ⇒ equalsF(v, qf)
    case qf: QueryFunction.exists  ⇒ existsF(v, qf)
    case qf: QueryFunction.range   ⇒ rangeF(v, qf)
    case qf: QueryFunction.raw     ⇒ rawF(v, qf)
    case qf: QueryFunction.bool    ⇒ boolF(v, qf)
  }

  def matchesF(v: V, qf: QueryFunction.matches): F[V]

  def equalsF(v: V, qf: QueryFunction.equals): F[V]

  def existsF(v: V, qf: QueryFunction.exists): F[V]

  def rangeF(v: V, qf: QueryFunction.range): F[V]

  def rawF(v: V, qf: QueryFunction.raw): F[V]

  def boolF(v: V, qf: QueryFunction.bool): F[V]
}

object QueryInterpreter {
  @inline implicit def apply[F[_], V](implicit qi: QueryInterpreter[F, V]): QueryInterpreter[F, V] = qi
}
