package foxcomm

import io.circe.generic.extras.Configuration
import io.circe.{Json, Printer}
import monix.eval.Task
import org.elasticsearch.action.ActionListener
import scala.concurrent.Promise

package object agni {
  implicit val configuration: Configuration =
    Configuration.default.withDefaults.withDiscriminator("type").withSnakeCaseKeys

  @SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
  def async[A, B](action: ActionListener[A] ⇒ Any): Task[A] = Task.deferFuture {
    val p = Promise[A]()
    action(new ActionListener[A] {
      def onFailure(e: Throwable): Unit = p.tryFailure(e)

      def onResponse(response: A): Unit = p.trySuccess(response)
    })
    p.future
  }

  implicit class RichJson(val j: Json) extends AnyVal {
    def dump: Array[Byte] = Printer.noSpaces.prettyByteBuffer(j).array()
  }
}
