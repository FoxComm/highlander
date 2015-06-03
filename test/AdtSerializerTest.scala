import org.json4s
import org.json4s.JsonAST.JString
import org.json4s.{CustomSerializer, DefaultFormats, JValue, Serializer}
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{FreeSpec, MustMatchers}

class AdtSerializerTest  extends FreeSpec with MustMatchers with TypeCheckedTripleEquals {
  import org.json4s.jackson.JsonMethods.parse
  import org.json4s.jackson.Serialization.write

  "Adt serialization" - {
    "with a CustomSerializer" in {
      def adtSerializer[T : Manifest] = () => {
        new CustomSerializer[T](format => (
          { case _ ⇒ ??? },
          /** You’ve missed the `: T` type check  here.*/
          { case x: T ⇒ JString(x.toString) }))
      }

      implicit val formats = DefaultFormats + adtSerializer[PaymentStatus].apply()

      val ast = parse(write(Map("status" → Auth)))
      (ast \ "status").extract[String] must === ("Auth")
    }

    "with a base trait" in {
      trait AdtBase

      sealed trait PaymentStatus2 extends AdtBase
      case object Auth2 extends PaymentStatus2
      case object FailedCapture2 extends PaymentStatus2

      implicit val formats = DefaultFormats + new CustomSerializer[AdtBase](format ⇒ (
        { case _ ⇒ ??? },
        { case x: AdtBase ⇒ JString(x.toString) }))

      val ast = parse(write(Map("status" → Auth2)))
      (ast \ "status").extract[String] must === ("Auth2")

      val ast2 = parse(write(Map("status" → FailedCapture2)))
      (ast2 \ "status").extract[String] must === ("FailedCapture2")
    }

    "with a low-level org.json4s.serializer" in {
      implicit val formats = DefaultFormats + new Serializer[PaymentStatus] {
        override def deserialize(implicit format: json4s.Formats) = ???

        override def serialize(implicit format: json4s.Formats): PartialFunction[Any, JValue] = {
          case t: PaymentStatus ⇒ JString(t.toString)
        }
      }

      val ast = parse(write(Map("status" → Auth)))
      (ast \ "status").extract[String] must === ("Auth")

      val ast2 = parse(write(Map("status" → FailedCapture)))
      (ast2 \ "status").extract[String] must === ("FailedCapture")
    }
  }
}
