package foxcomm.agni

import cats.~>
import monix.eval.{Coeval, Task}

package object interpreter {
  implicit val coevalToTask: Coeval ~> Task = new (Coeval ~> Task) {
    def apply[A](fa: Coeval[A]): Task[A] = fa.task
  }
}
