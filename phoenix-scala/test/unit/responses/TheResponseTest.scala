package responses

import cats.laws.MonadLaws
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import testutils.TestBase

/**
  * Created by mw on 2/9/17.
  */
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

      // TODO: finish the laws … maybe. But we’re abandoning TheResponse anyways.
     }
  }

}
