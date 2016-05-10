package consumer.activity

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.ElasticDsl.{mapping ⇒ esMapping}
import com.sksamuel.elastic4s.mappings.FieldType._

import scala.concurrent.Future
import scala.language.postfixOps

import org.json4s.JsonAST.JInt
import org.json4s.jackson.JsonMethods.parse
import org.json4s.DefaultFormats

import consumer.aliases._
import consumer.elastic.JsonTransformer
import consumer.elastic.MappingHelpers._

import consumer.utils.PhoenixConnectionInfo
import consumer.utils.Phoenix
import consumer.utils.HttpResponseExtensions._

final case class ActivityConnectionTransformer(conn: PhoenixConnectionInfo)
  (implicit ec: EC, mat: AM, ac: AS, cp: CP, sc: SC) extends JsonTransformer {

  implicit val formats: DefaultFormats.type = DefaultFormats

  val phoenix = Phoenix(conn)

  def mapping() = esMapping("activity_connections").fields(
    field("id", IntegerType),
    field("dimensionId", IntegerType),
    field("objectId", StringType).index("not_analyzed"),
    field("trailId", IntegerType),
    field("activity").nested(
      field("id", IntegerType),
      field("createdAt", DateType).format(dateFormat),
      field("kind", StringType).index("not_analyzed"),
      field("context").nested(
        field("transactionId", StringType).index("not_analyzed"),
        field("userId", IntegerType),
        field("userType", StringType).index("not_analyzed")
      ),
      field("data", ObjectType)
    ),
    field("previousId", IntegerType),
    field("nextId", IntegerType),
    field("data", ObjectType),
    field("connectedBy", ObjectType),
    field("createdAt", DateType).format(dateFormat)
  )

  def transform(json: String) : Future[String] = {
    Console.out.println(json)

    parse(json) \ "id" \ "int" match {
      case JInt(id) ⇒ queryPhoenixForConnection(id)
      case _        ⇒ throw new IllegalArgumentException("Activity connection is missing id")
    }
  }

  private def queryPhoenixForConnection(id: BigInt) : Future[String] = {
    val uri = s"connections/$id"
    Console.err.println(s"Requesting Phoenix $uri")
    phoenix.get(uri).flatMap(_.bodyText)
  }

}
