package services

import collection.immutable
import com.stripe.exception.StripeException
import utils.{ModelWithIdParameter, Validation}
import utils.friendlyClassName

sealed trait Failure {
  def description: immutable.Traversable[String]
}

final case class NotFoundFailure(message: String) extends Failure {
  override def description = List(message)
}

object NotFoundFailure {
  def apply[M <: ModelWithIdParameter](m: M): NotFoundFailure =
    NotFoundFailure(s"${m.modelName} with id=${m.id} not found")

  def apply[A](a: A, id: Int): NotFoundFailure = NotFoundFailure(friendlyClassName(a))
}

final case class StripeFailure(exception: StripeException) extends Failure {
  override def description = List(exception.getMessage)
}

final case class ValidationFailure(violation: Validation.Result.Failure) extends Failure {
  override def description = violation.messages.map(_.toString)
}

final case class GeneralFailure(a: String) extends Failure {
  override def description = List(a)
}
