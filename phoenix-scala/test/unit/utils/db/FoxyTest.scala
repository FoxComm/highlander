package utils.db

import cats._
import cats.data._
import cats.implicits._
import testutils.TestBase
import failures.{Failure, GeneralFailure}

class FoxyTest extends TestBase {
  "Monads should" - {
    "work" in {

      type FoxyTOption[A] = FoxyT[Option, A]
      object FoxyTOption extends FoxyTOps[Option]

      import FoxyTOption._
      val res = for {

        x ← fromId {
             import Foxy._
             for {
               a ← pure(6)
               _ ← warning(GeneralFailure("hiyaaaaa from FoxyT[Id]!"))
               b ← pure(2)
               //_ ← failure(Failure("noes, FoxyT[Id] was fatal"))
             } yield (a / b)
           }

        _ ← warning(GeneralFailure("hello from FoxyT[Option]"))
        y ← pure(3)
        _ ← warning(GeneralFailure("s’more from FoxyT[Option]"))
        //_ ← failure(Failure("FoxyT[Option] was fatal, sorry"))
        _ ← failWithMatchedWarning {
             case GeneralFailure(ζ) if ζ contains "heeello" ⇒ true
           }
        _ ← {
          info("→→→ the flow got to the end ←←←")
          pure(())
        }
      } yield y

      info(s"${res.runEmpty.value}")
    }
  }
}
