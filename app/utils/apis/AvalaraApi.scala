package utils.apis

import java.time.Instant

import scala.collection.immutable._
import scala.concurrent.Future
import akka.http.scaladsl.Http
import scala.util.{Failure, Success}
import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.stream.scaladsl._
import akka.stream.ActorMaterializer

import com.pellucid.sealerate
import services.Result
import utils.ADT
import utils.FoxConfig._
import utils.aliases.EC

trait AvalaraApi {

  def estimateTax: Result[Unit]
  def getTax()(implicit ec: EC): Result[Unit]

}

object AvalaraApiRequests {}

object AvalaraApiResponses {
  sealed trait SeverityLevel
  case object Success   extends SeverityLevel
  case object Warning   extends SeverityLevel
  case object Error     extends SeverityLevel
  case object Exception extends SeverityLevel

  object SeverityLevel extends ADT[SeverityLevel] {
    def types = sealerate.values[SeverityLevel]
  }

  case class Message(
      Summary: String,
      Details: String,
      RefersTo: String,
      Severity: SeverityLevel,
      Source: String
  )

  case class TaxAddress(
      Address: String,
      AddressCode: String,
      City: String,
      Region: String,
      Country: String,
      PostalCode: String,
      Latitude: String,
      Longitude: String,
      TaxRegionId: String,
      JurisCode: String
  )

  case class TaxDetail(
      Rate: Double,
      Tax: Double,
      Taxable: Double,
      Country: String,
      Region: String,
      JurisType: String,
      JurisName: String,
      TaxName: String
  )

  case class TaxLine(
      LineNo: String,
      TaxCode: String,
      Taxability: Boolean,
      Taxable: Double,
      Rate: Double,
      Tax: Double,
      Discount: Double,
      TaxCalculated: Double,
      Exemption: Double,
      TaxDetails: Seq[TaxDetail],
      BoundaryLevel: String
  )

  case class GetTaxes(
      docCode: String,
      docDate: Instant,
      timestamp: Instant,
      totalAmount: Double,
      TotalDiscount: Double,
      TotalExemption: Double,
      TotalTaxable: Double,
      TotalTax: Double,
      TotalTaxCalculated: Double,
      TaxDate: Instant,
      TaxLines: Seq[TaxLine],
      TaxSummary: Seq[TaxLine],
      TaxAddresses: Seq[TaxAddress],
      ResultCode: SeverityLevel,
      Messages: Seq[Message]
  )
}

class Avalara()(implicit as: ActorSystem, am: ActorMaterializer) extends AvalaraApi {
  private def getConfig(): (String, String, String, String) = {
    val url     = config.getString("avalara.url")
    val account = config.getString("avalara.account")
    val license = config.getString("avalara.license")
    val profile = config.getString("avalara.profile")
    (url, account, license, profile)
  }

  override def estimateTax: Result[Unit] = Result.unit

  override def getTax()(implicit ec: EC): Result[Unit] = {
    val (url, account, license, profile) = getConfig()
    val connectionFlow: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]] =
      Http().outgoingConnectionHttps(url)
    val headers: Seq[HttpHeader] = Seq(Authorization(BasicHttpCredentials(account, license)))
    val responseFuture: Future[HttpResponse] = Source
      .single(HttpRequest(uri = "/1.0/tax/get", method = HttpMethods.POST, headers = headers))
      .via(connectionFlow)
      .runWith(Sink.head)

    responseFuture.andThen {
      case Success(_) ⇒ println("request succeded")
      case Failure(_) ⇒ println("request failed")
    }

    Result.unit
  }
}
