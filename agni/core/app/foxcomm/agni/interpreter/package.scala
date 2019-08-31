package foxcomm.agni

import scala.language.higherKinds

package object interpreter {
  type Interpreter[F[_], A, B] = A ⇒ F[B]
}
