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

case class SkuInventoryHold(sku: String, qty: Int)
case class OrderInventoryHold(refNum: String, items: Seq[SkuInventoryHold])

trait MiddlewarehouseApi {

  implicit val formats = JsonFormatters.phoenixFormats

  def hold(reservation: OrderInventoryHold)(implicit ec: EC): Result[Unit]
  def cancelHold(orderRefNum: String)(implicit ec: EC): Result[Unit]

}

class Middlewarehouse(url: String) extends MiddlewarehouseApi with LazyLogging {

  override def hold(reservation: OrderInventoryHold)(implicit ec: EC): Result[Unit] = {

    val reqUrl = dispatch.url(s"$url/v1/private/reservations/hold")
    val body   = compact(Extraction.decompose(reservation))
    val req    = reqUrl.setContentType("application/json", "UTF-8") << body
    logger.info(s"middlewarehouse hold: $body")
    Http(req.POST OK as.String).either.flatMap {
      case Right(_)    ⇒ Result.unit
      case Left(error) ⇒ Result.failure(MiddlewarehouseFailures.UnableToHoldLineItems)
    }
  }

  //Note cart ref becomes order ref num after cart turns into order
  override def cancelHold(orderRefNum: String)(implicit ec: EC): Result[Unit] = {

    val reqUrl = dispatch.url(s"$url/v1/private/reservations/hold/${orderRefNum}")
    val req    = reqUrl.setContentType("application/json", "UTF-8")
    logger.info(s"middlewarehouse cancel hold: ${orderRefNum}")
    Http(req.DELETE OK as.String).either.flatMap {
      case Right(_)    ⇒ Result.unit
      case Left(error) ⇒ Result.failure(MiddlewarehouseFailures.UnableToCancelHoldLineItems)
    }
  }
}
