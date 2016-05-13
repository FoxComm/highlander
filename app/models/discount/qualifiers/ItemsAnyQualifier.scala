package models.discount.qualifiers

import cats.data.Xor
import failures.DiscountFailures._
import models.discount.{DiscountInput, SearchReference}
import services.Result
import utils.aliases._

case class ItemsAnyQualifier(search: SearchReference) extends Qualifier {

  def check(input: DiscountInput)(implicit ec: EC, es: ES): Result[Unit] = {
    val future = for { result ← SearchReference.query(input, search) } yield result

    Result.fromFuture(future.map {
      case Xor.Right(count) if count > 0 ⇒ Xor.Right(Unit)
      case _                             ⇒ Xor.Left(SearchFailure)
    })
  }
}
