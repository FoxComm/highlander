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
case class OrderReservation(refNum: String, reservations: Seq[SkuReservation])

trait MiddlewarehouseApi {

  implicit val formats = JsonFormatters.phoenixFormats

  def reserve(reservation: OrderReservation)(implicit ec: EC): Result[Unit]
}

class Middlewarehouse(url: String) extends MiddlewarehouseApi with LazyLogging {

  override def reserve(reservation: OrderReservation)(implicit ec: EC): Result[Unit] = {
    // TODO: make request, parse, check for errors
    // If response status code is BadRequest and/or errors are present in response, wrap as Xor.left to fail checkout

    val reqUrl = dispatch.url(s"$url/reservations/reserve")
    val body   = compact(Extraction.decompose(reservation))
    val req    = reqUrl.setContentType("application/json", "UTF-8") << body
    logger.info(s"middlewarehouse reservation: $reqUrl : $body")
    Http(req.POST OK as.String).either.flatMap {
      case Right(_)    ⇒ Result.unit
      case Left(error) ⇒ Result.failure(MiddlewarehouseFailures.UnableToReserveLineItems(error))
    }
  }
}
