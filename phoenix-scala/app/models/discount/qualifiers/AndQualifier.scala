package models.discount.qualifiers

import scala.concurrent.Future

import cats.data.{NonEmptyList, Xor}
import cats.std.list._
import models.account.User
import models.discount.DiscountInput
import services.Authenticator.AuthData
import services.Result
import utils.aliases._

case class AndQualifier(qualifiers: Seq[Qualifier]) extends Qualifier {

  val qualifierType: QualifierType = And

  def check(input: DiscountInput)(implicit db: DB,
                                  ec: EC,
                                  es: ES,
                                  auth: AuthData[User]): Result[Unit] = {
    val checks = Future.sequence(qualifiers.map(_.check(input)))

    checks.map(seq ⇒ seq.flatMap(_.fold(fs ⇒ fs.unwrap, q ⇒ Seq.empty))).map {
      case head :: tail ⇒ Xor.Left(NonEmptyList(head, tail))
      case Nil          ⇒ Xor.Right(Unit)
    }
  }
}
