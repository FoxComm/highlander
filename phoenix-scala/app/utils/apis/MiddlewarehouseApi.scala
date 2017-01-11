package utils.apis

import com.github.levkhomich.akka.tracing.http.TracingHeaders._
import com.ning.http.client
import com.typesafe.scalalogging.LazyLogging
import dispatch._
import failures.MiddlewarehouseFailures.MiddlewarehouseError
import failures.{Failures, MiddlewarehouseFailures}
import org.json4s.Extraction
import org.json4s.jackson.JsonMethods._
import services.Result
import utils.JsonFormatters
import payloads.AuthPayload
import utils.aliases._

case class SkuInventoryHold(sku: String, qty: Int)
case class OrderInventoryHold(refNum: String, items: Seq[SkuInventoryHold])

trait MiddlewarehouseApi {

  implicit val formats = JsonFormatters.phoenixFormats

  def hold(
      reservation: OrderInventoryHold)(implicit ec: EC, au: AU, tr: TR, tracer: TEI): Result[Unit]
  def cancelHold(orderRefNum: String)(implicit ec: EC, au: AU, tr: TR, tracer: TEI): Result[Unit]

  def tracedHeaders(implicit tr: TR, tracer: TEI): Traversable[(String, String)] = {
    tracer
      .exportMetadata(tr)
      .map(span ⇒
            Map(TraceId → span.traceId.toHexString,
                SpanId  → span.spanId.toHexString,
                Sampled → "1"))
      .getOrElse(Nil)
  }

}

class Middlewarehouse(url: String) extends MiddlewarehouseApi with LazyLogging {

  def parseMwhErrors(message: String): Failures = {
    val errorString = (parse(message) \ "errors").extractOpt[List[String]]
    errorString
      .flatMap(errors ⇒ Failures(errors.map(MiddlewarehouseError): _*))
      .getOrElse(MiddlewarehouseError(message).single)
  }

  override def hold(reservation: OrderInventoryHold)(implicit ec: EC,
                                                     au: AU,
                                                     tr: TR,
                                                     tracer: TEI): Result[Unit] = {
    val reqUrl  = dispatch.url(s"$url/v1/private/reservations/hold")
    val body    = compact(Extraction.decompose(reservation))
    val jwt     = AuthPayload.jwt(au.token)
    val headers = tracedHeaders ++ Map("JWT" → jwt)

    val req = reqUrl.setContentType("application/json", "UTF-8") <:< headers << body

    logger.info(s"middlewarehouse hold: $body")

    Http(req.POST > AsMwhResponse).either.flatMap {
      case Right(MwhResponse(status, _)) if status / 100 == 2 ⇒ Result.unit
      case Right(MwhResponse(_, message))                     ⇒ Result.failures(parseMwhErrors(message))
      case Left(error)                                        ⇒ Result.failure(MiddlewarehouseFailures.UnableToHoldLineItems)
    }
  }

  //Note cart ref becomes order ref num after cart turns into order
  override def cancelHold(
      orderRefNum: String)(implicit ec: EC, au: AU, tr: TR, tracer: TEI): Result[Unit] = {

    val reqUrl  = dispatch.url(s"$url/v1/private/reservations/hold/${orderRefNum}")
    val jwt     = AuthPayload.jwt(au.token)
    val headers = tracedHeaders ++ Map("JWT" → jwt)
    val req     = reqUrl.setContentType("application/json", "UTF-8") <:< headers
    logger.info(s"middlewarehouse cancel hold: ${orderRefNum}")
    Http(req.DELETE OK as.String).either.flatMap {
      case Right(_)    ⇒ Result.unit
      case Left(error) ⇒ Result.failure(MiddlewarehouseFailures.UnableToCancelHoldLineItems)
    }
  }
}

case class MwhResponse(statusCode: Int, content: String)

object AsMwhResponse extends (client.Response ⇒ MwhResponse) {

  override def apply(r: client.Response): MwhResponse =
    MwhResponse(r.getStatusCode, r.getResponseBody)

}
