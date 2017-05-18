package responses

import cats.laws.MonadLaws
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import testutils.TestBase

class TheResponseTest extends TestBase with GeneratorDrivenPropertyChecks {

  "TheResponse" - {
    "should obey monad laws" - {
      val laws = MonadLaws[TheResponse]

      def insane[A](xs: List[A]): Option[List[A]] =
        xs match {
          case Nil ⇒ None
          case _   ⇒ Some(xs)
        }

      "left identity" in forAll { (a: Int, warnings: List[String], f: Int ⇒ Long) ⇒
        val isEq =
          laws.monadLeftIdentity(a, f andThen (TheResponse(_, warnings = insane(warnings))))
        isEq.lhs must === (isEq.rhs)
      }

      // TODO: finish the laws … maybe. But we’re abandoning TheResponse shortly anyways,
      // for a new, Writer-based monad. I hope the change won’t touch many lines initially.
    }
  }

}
