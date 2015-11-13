package utils

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions

import cats.data.Xor
import services.{Failure, Failures, DatabaseFailure, CustomerEmailNotUnique, Result}
import scala.util.matching.Regex

object jdbc {

  sealed trait FailureSwap
  case object NotUnique extends FailureSwap

  val constraints: Map[FailureSwap, Regex] = Map (
    NotUnique → """ERROR: duplicate key value violates unique constraint""".r
  )

  @SuppressWarnings(Array("org.brianmckenna.wartremover.warts.IsInstanceOf", "org.brianmckenna.wartremover.warts.AsInstanceOf"))
  def swapDatabaseFailure[A](result: Result[A])(failed: (FailureSwap, Failure)) // TODO (FailureSwap, Failure)*
    (implicit ec: ExecutionContext): Result[A] = {
    result.map(_.leftMap { currentFailures ⇒ (currentFailures.head, failed) match {
        case (dbFailure: DatabaseFailure, (symbol, replacement)) ⇒
          constraints.get(symbol).map { regex ⇒
            if (regex.findFirstIn(dbFailure.description.head).nonEmpty) replacement.single
            else dbFailure.single
          }.getOrElse(dbFailure.single)
        case (otherFailure, _) ⇒
          otherFailure.single
    }})
  }
}
