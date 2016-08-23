package utils.apis

import com.typesafe.scalalogging.LazyLogging
import org.json4s.jackson.Serialization.write
import services.Result
import utils.JsonFormatters
import utils._
import utils.aliases._
import utils.FoxConfig.{RichConfig, config}
import org.json4s.jackson.JsonMethods._
import org.json4s.Extraction
import failures.MiddlewarehouseFailures

import dispatch._

case class SkuReservation(sku: String, qty: Int)
case class OrderReservation(refNum: String, items: Seq[SkuReservation])

trait MiddlewarehouseApi {

  implicit val formats = JsonFormatters.phoenixFormats

  def reserve(reservation: OrderReservation)(implicit ec: EC): Result[Unit]
}

class Middlewarehouse(url: String) extends MiddlewarehouseApi with LazyLogging {

  override def reserve(reservation: OrderReservation)(implicit ec: EC): Result[Unit] = {

    val reqUrl = dispatch.url(s"$url/v1/private/reservations/hold")
    val body   = compact(Extraction.decompose(reservation))
    val req    = reqUrl.setContentType("application/json", "UTF-8") << body
    logger.info(s"middlewarehouse reservation: $reqUrl : $body")
    Http(req.POST OK as.String).either.flatMap {
      case Right(_)    ⇒ Result.unit
      case Left(error) ⇒ Result.failure(MiddlewarehouseFailures.UnableToReserveLineItems(error))
    }
  }
}
