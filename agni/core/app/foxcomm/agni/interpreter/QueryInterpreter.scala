package foxcomm.agni.interpreter

import scala.language.higherKinds
import cats.data.NonEmptyList
import foxcomm.agni.dsl.query._

sealed trait QueryError
object QueryError {}

trait QueryInterpreter[F[_], V] extends Interpreter[F, NonEmptyList[QueryFunction], V] {
  type Result = V

  final def eval(qf: QueryFunction): F[Result] = qf match {
    case qf: QueryFunction.matches ⇒ matchesF(qf)
    case qf: QueryFunction.equals  ⇒ equalsF(qf)
    case qf: QueryFunction.exists  ⇒ existsF(qf)
    case qf: QueryFunction.range   ⇒ rangeF(qf)
    case qf: QueryFunction.raw     ⇒ rawF(qf)
    case qf: QueryFunction.bool    ⇒ boolF(qf)
  }

  def matchesF(qf: QueryFunction.matches): F[Result]

  def equalsF(qf: QueryFunction.equals): F[Result]

  def existsF(qf: QueryFunction.exists): F[Result]

  def rangeF(qf: QueryFunction.range): F[Result]

  def rawF(qf: QueryFunction.raw): F[Result]

  def boolF(qf: QueryFunction.bool): F[Result]
}

object QueryInterpreter {
  @inline implicit def apply[F[_], V](implicit qi: QueryInterpreter[F, V]): QueryInterpreter[F, V] = qi
}
