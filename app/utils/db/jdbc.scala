package utils

import scala.language.implicitConversions
import scala.util.matching.Regex

import failures.{DatabaseFailure, Failure}
import services.Result
import utils.aliases.EC

object jdbc {

  sealed trait FailureSwap
  case object NotUnique extends FailureSwap

  val constraints: Map[FailureSwap, Regex] = Map(
      NotUnique → """ERROR: duplicate key value violates unique constraint""".r
  )

  // TODO (FailureSwap, Failure)*
  def swapDatabaseFailure[A](result: Result[A])(failed: (FailureSwap, Failure))(
      implicit ec: EC): Result[A] = {
    result.map(_.leftMap { currentFailures ⇒
      (currentFailures.head, failed) match {
        case (dbFailure: DatabaseFailure, (symbol, replacement)) ⇒
          constraints
            .get(symbol)
            .map { regex ⇒
              if (regex.findFirstIn(dbFailure.description).nonEmpty) replacement.single
              else dbFailure.single
            }
            .getOrElse(dbFailure.single)
        case (otherFailure, _) ⇒
          otherFailure.single
      }
    })
  }
}
