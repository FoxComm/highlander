package utils.db

import cats._
import cats.data._
import cats.implicits._
import testutils.TestBase
import failures.Failure

object Blah {

  final case class StringFailure(description: String) extends Failure

  type FoxyTOption[A] = FoxyT[Option, A]
  object FoxyTOption extends FoxyTOps[Option]

  final case class JsonResponse[A](result: Option[A],
                                   warnings: Option[List[Failure]], // really? 🙄
                                   error: Option[Failure])

  object JsonResponse {
    def fromFoxyTOption[A](fa: FoxyTOption[A]): JsonResponse[A] =
      fa.run(Nil).value match {
        case Some(Xor.Left(error)) ⇒
          JsonResponse(result = None, warnings = None, error = Some(error))
        case Some(Xor.Right((Nil, result))) ⇒
          JsonResponse(result = Some(result), warnings = None, error = None)
        case Some(Xor.Right((warnings, result))) ⇒
          JsonResponse(result = Some(result), warnings = Some(warnings.collect {
            case UIInfo.Warning(f) ⇒ f
          }), error = None)
        case _ ⇒
          JsonResponse(result = None,
                       warnings = None,
                       error = Some(StringFailure("Woot, shouldn’t have happened⸮!")))
      }
  }

}

import Blah._

class FoxyTest extends TestBase {
  "Monads should" - {
    "work" in {

      import FoxyTOption._
      val res = for {

        x ← fromId {
             import Foxy._
             for {
               a ← pure(6)
               _ ← warning(StringFailure("hiyaaaaa from FoxyT[Id]!"))
               b ← pure(2)
               //_ ← failure(Failure("noes, FoxyT[Id] was fatal"))
             } yield (a / b)
           }

        _ ← warning(StringFailure("hello from FoxyT[Option]"))
        y ← pure(3)
        _ ← warning(StringFailure("s’more from FoxyT[Option]"))
        //_ ← failure(Failure("FoxyT[Option] was fatal, sorry"))
        _ ← failWithMatchedWarning {
             case StringFailure(ζ) if ζ contains "heeello" ⇒ true
           }
        _ ← {
          info("→→→ the flow got to the end ←←←")
          pure(())
        }
      } yield y

      info(s"${JsonResponse.fromFoxyTOption(res)}")
    }
  }
}
