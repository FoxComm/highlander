package utils.db

import cats._
import cats.data._
import cats.implicits._
import testutils.TestBase

object Blah {

  final case class Failure(ζ: String) extends AnyVal

  /* We can’t use WriterT for warnings, because of the `failWithMatchedWarning`. */
  type FoxyT[F[_], A] = StateT[XorT[F, Failure, ?], List[Failure], A]

  type Foxy[A] = FoxyT[Id, A]
  object Foxy extends FoxyTOps[Id]

  type FoxyTOption[A] = FoxyT[Option, A]
  object FoxyTOption extends FoxyTOps[Option]

  trait FoxyTOps[F[_]] {
    def apply[A](a: A)(implicit M: Monad[F]): FoxyT[F, A] =
      pure(a)

    def pure[A](a: A)(implicit M: Monad[F]): FoxyT[F, A] =
      Monad[FoxyT[F, ?]].pure(a)

    def warning(f: Failure)(implicit M: Monad[F]): FoxyT[F, Unit] =
      StateT.modify(f :: _)

    def failure(f: Failure)(implicit M: Monad[F]): FoxyT[F, Unit] =
      StateT(_ ⇒ XorT.left(M.pure(f)))

    def fromId[A](fa: Foxy[A])(implicit M: Monad[F]): FoxyT[F, A] =
      StateT(s ⇒ XorT(M.pure(fa.run(s).value)))

    def failWithMatchedWarning(pf: PartialFunction[Failure, Boolean])(
        implicit M: Monad[F]): FoxyT[F, Unit] =
      StateT(s ⇒
            s.find(pf.lift(_) == Some(true)) match {
          case Some(f) ⇒ XorT.left(M.pure(f))
          case _       ⇒ XorT.right(M.pure((s, ())))
      })
  }

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
          JsonResponse(result = Some(result), warnings = Some(warnings), error = None)
        case _ ⇒
          JsonResponse(result = None,
                       warnings = None,
                       error = Some(Failure("Woot, shouldn’t have happened⸮!")))
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
               _ ← warning(Failure("hiyaaaaa from FoxyT[Id]!"))
               b ← pure(2)
               //_ ← failure(Failure("noes, FoxyT[Id] was fatal"))
             } yield (a / b)
           }

        _ ← warning(Failure("hello from FoxyT[Option]"))
        y ← pure(3)
        _ ← warning(Failure("s’more from FoxyT[Option]"))
        //_ ← failure(Failure("FoxyT[Option] was fatal, sorry"))
        _ ← failWithMatchedWarning {
             case Failure(ζ) if ζ contains "heeello" ⇒ true
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
