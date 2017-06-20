package foxcomm

import cats.Monad
import io.circe.generic.extras.Configuration
import monix.cats._
import monix.eval.{Coeval, Task}
import org.elasticsearch.action.ActionListener
import scala.concurrent.Promise

package object agni {
  implicit val configuration: Configuration =
    Configuration.default.withDiscriminator("type").withSnakeCaseKeys

  // FIXME: For now we cache converted monix to cats monad in order to save unnecessary allocation on every call.
  // Should be ready to remove once monix 3.0.x will become stable.
  implicit val coevalMonad: Monad[Coeval] = monixToCatsMonad[Coeval]

  @SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
  def async[A, B](action: ActionListener[A] ⇒ Any): Task[A] = Task.deferFuture {
    val p = Promise[A]()
    action(new ActionListener[A] {
      def onFailure(e: Throwable): Unit = p.tryFailure(e)

      def onResponse(response: A): Unit = p.trySuccess(response)
    })
    p.future
  }
}
