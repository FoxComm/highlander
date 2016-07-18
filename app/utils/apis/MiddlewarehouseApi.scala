package utils.apis

import com.typesafe.scalalogging.LazyLogging
import org.json4s.jackson.Serialization.write
import services.Result
import utils.JsonFormatters

case class OrderReservation(referenceNumber: String, reservations: Seq[SkuReservation])
case class SkuReservation(sku: String, quantity: Int)

trait MiddlewarehouseApi {

  implicit val formats = JsonFormatters.phoenixFormats

  def reserve(reservation: OrderReservation): Result[Unit]
}

class Middlewarehouse extends MiddlewarehouseApi with LazyLogging {
  // TODO: load configuration

  override def reserve(reservation: OrderReservation): Result[Unit] = {
    // TODO: make request, parse, check for errors
    // If response status code is BadRequest and/or errors are present in response, wrap as Xor.left to fail checkout
    logger.info("middlewarehouse", write(reservation))
    Result.unit
  }
}
