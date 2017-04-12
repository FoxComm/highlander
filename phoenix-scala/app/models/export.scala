package models

import akka.NotUsed
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.stream.scaladsl.Source
import org.json4s.JsonAST.{JNumber, JObject, JString}
import scala.util.{Success, Try}
import utils.Chunkable
import utils.aliases._
import utils.http.Http

sealed trait ExportFormat {
  type Type

  def chunkify(fields: List[String], separator: String): Source[Json, NotUsed] ⇒ HttpResponse
}
object ExportFormat {
  val unmarshaller: Unmarshaller[String, ExportFormat] = Unmarshaller.strict {
    case "csv"  ⇒ CSV
    case "json" ⇒ JSON
    case x      ⇒ throw new IllegalArgumentException(s"'$x' is not a valid export format")
  }

  case object CSV extends ExportFormat {
    type Type = CsvData

    def chunkify(fields: List[String], separator: String): Source[Json, NotUsed] ⇒ HttpResponse = {
      def convert(obj: JObject): Type = {
        val objFields = obj.obj.toMap
        fields.flatMap(field ⇒
              objFields.get(field).collect {
            case jn: JNumber ⇒ field → s""""${jn.values}""""
            case js: JString ⇒ field → s""""${js.values.replace("\"", "\"\"")}""""
        })(collection.breakOut)
      }

      source ⇒
        {
          implicit val chunkable = Chunkable.csvChunkable(fields, separator)

          Http.renderAttachment(source.collect {
            case obj: JObject ⇒ convert(obj)
          })
        }
    }
  }
  case object JSON extends ExportFormat {
    type Type = Json

    def chunkify(fields: List[String], separator: String): Source[Json, NotUsed] ⇒ HttpResponse =
      Http.renderAttachment(_)
  }
}
