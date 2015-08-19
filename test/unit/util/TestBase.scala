package util

import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FreeSpecLike, FreeSpec, MustMatchers}
import utils.Config._
import cats.data.Xor

trait TestBase extends FreeSpecLike
  with MustMatchers
  with ScalaFutures
  with TypeCheckedTripleEquals {

  val config = TestBase.config

  /**
   * Asserts that the passed Xor is a Right, and passes the right value
   * to the given function.
   */
  def rightValue[L, R, Z](xor: Xor[L, R])(onRight: R ⇒ Z): Z = xor match {
    case l: Xor.Left[_]   ⇒ fail(s"Unexpected Xor.Left: $l")
    case Xor.Right(right) ⇒ onRight(right)
  }
}

object TestBase {
  def config = utils.Config.loadWithEnv(env = Test)
}
