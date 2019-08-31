package foxcomm

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.dataformat.smile.SmileFactory
import io.circe.generic.extras.Configuration
import io.circe.{Json, Printer}
import java.io.ByteArrayOutputStream
import monix.eval.Task
import org.elasticsearch.action.ActionListener
import scala.concurrent.Promise

package object agni {
  private[this] val smileFactory = new SmileFactory()
  private[this] val jsonFactory  = new JsonFactory()

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

  @SuppressWarnings(Array("org.wartremover.warts.While"))
  implicit class RichJson(val j: Json) extends AnyVal {
    def toBytes: Array[Byte] = Printer.noSpaces.prettyByteBuffer(j).array()

    def toSmile: Array[Byte] = {
      val bos = new ByteArrayOutputStream()
      val jg  = smileFactory.createGenerator(bos)
      val jp  = jsonFactory.createParser(j.toBytes)
      try while (jp.nextToken() ne null) {
        jg.copyCurrentEvent(jp)
      } finally {
        jp.close()
        jg.close()
      }
      bos.toByteArray
    }
  }
}
