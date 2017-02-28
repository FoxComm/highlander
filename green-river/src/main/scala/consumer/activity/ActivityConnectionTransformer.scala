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
import consumer.utils.PhoenixConnectionInfo
import consumer.utils.Phoenix
import consumer.utils.HttpResponseExtensions._
import consumer.AvroJsonHelper
import cats.implicits._
import cats.data.XorT
import consumer.failures.Failures
import consumer.elastic.mappings.dateFormat

final case class ActivityConnectionTransformer(
    conn: PhoenixConnectionInfo)(implicit ec: EC, mat: AM, ac: AS, cp: CP, sc: SC)
    extends JsonTransformer {

  implicit val formats: DefaultFormats.type = DefaultFormats

  val phoenix = Phoenix(conn)

  val topic = "scoped_activity_trails"

  def mapping() = esMapping(topic).fields(
      field("id", IntegerType),
      field("dimension", StringType),
      field("objectId", StringType) index "not_analyzed",
      field("activity").nested(
          field("id", StringType),
          field("createdAt", DateType) format dateFormat,
          field("kind", StringType) index "not_analyzed",
          field("context").nested(
              field("transactionId", StringType) index "not_analyzed",
              field("userId", IntegerType),
              field("userType", StringType) index "not_analyzed"
          ),
          field("data", ObjectType)
      ),
      field("scope", StringType) index "not_analyzed",
      field("createdAt", DateType) format dateFormat
  )

  val jsonFields = List("id", "activity", "connectedBy")

  def transform(json: String): Future[String] = {
    Console.out.println(json)

    parse(json) \ "id" \ "long" match {
      case JInt(id) ⇒ Future { AvroJsonHelper.transformJson(json, jsonFields) }
      case _        ⇒ throw new IllegalArgumentException("Activity connection is missing id")
    }
  }
}
