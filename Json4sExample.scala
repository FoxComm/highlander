import org.json4s.JsonAST.JString
import org.json4s.{FieldSerializer, CustomSerializer, DefaultFormats}

object Json4sExample extends App {
  sealed trait PaymentStatus
  case object Auth extends PaymentStatus
  case object FailedCapture extends PaymentStatus
  case object CanceledAuth extends PaymentStatus
  case object ExpiredAuth extends PaymentStatus

  case class LineItem(id: Int, skuId: Int)
  case class Cart(id: Int, userId: Option[Int] = None, lineItems: Seq[LineItem])

  case class PaymentDemo(id: Int, status: PaymentStatus)

  /** Can be extracted in a trait and mixed in wherever we need it to provide that implicit format. */
  val phoenixFormats = DefaultFormats + new CustomSerializer[PaymentStatus](format => (
    { case _ ⇒ sys.error("Reading not implemented") },
    { case x: PaymentStatus ⇒ JString(x.toString) }
  ))

  import org.json4s.jackson.Serialization.write

  simpleExamples()
  transformationExamples()

  def simpleExamples() {
    implicit val formats = phoenixFormats

    println(write(
      Cart(id = 1, userId = Some(2), lineItems = Seq(LineItem(42, 48)))
    ))

    println(write(
      PaymentDemo(1, Auth)
    ))

    println(write(
      PaymentDemo(2, CanceledAuth)
    ))
  }

  def transformationExamples() {
    /** transform the AST before output */
    implicit val formatsAndTransformation = phoenixFormats + new FieldSerializer[Cart](
      FieldSerializer.renameTo("lineItems", "line_items"),
      FieldSerializer.renameFrom("lineItems", "line_items"))

    println(write(
      Cart(id = 1, userId = Some(2), lineItems = Seq(LineItem(42, 48)))
    ))
  }
}
