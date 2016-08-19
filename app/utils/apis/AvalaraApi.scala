package utils.apis

import java.time.Instant

import scala.collection.immutable._
import scala.concurrent.Future
import akka.http.scaladsl.Http
import scala.util.{Failure, Success}
import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.unmarshalling.{FromResponseUnmarshaller, Unmarshal, Unmarshaller}
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, Materializer}
import concurrent.duration._

import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson
import org.json4s.jackson.Serialization._
import com.pellucid.sealerate
import failures.AvalaraFailures._
import services.Result
import utils.{ADT, Money, time}
import utils.FoxConfig._
import utils.aliases.EC

trait AvalaraApi {

  def estimateTax: Result[Unit]
  def getTax()(implicit ec: EC): Result[Unit]

}

object Avalara {
  object Requests {
    sealed trait AddressType
    case object F extends AddressType //Firm or company address
    case object G extends AddressType //General Delivery address
    case object H extends AddressType //High-rise or business complex
    case object P extends AddressType //PO box address
    case object R extends AddressType //Rural route address
    case object S extends AddressType //Street or residential address, probably, we will mostly use it

    object AddressType extends ADT[AddressType] {
      def types = sealerate.values[AddressType]
    }

    sealed trait DetailLevel
    case object Tax        extends DetailLevel
    case object Document   extends DetailLevel
    case object Line       extends DetailLevel
    case object Diagnostic extends DetailLevel

    object DetailLevel extends ADT[DetailLevel] {
      def types = sealerate.values[DetailLevel]
    }

    sealed trait DocType
    case object SalesOrder           extends DocType
    case object SalesInvoice         extends DocType
    case object ReturnOrder          extends DocType
    case object ReturnInvoice        extends DocType
    case object PurchaseOrder        extends DocType
    case object PurchaseInvoice      extends DocType
    case object ReverseChargeOrder   extends DocType
    case object ReverseChargeInvoice extends DocType

    object DocType extends ADT[DocType] {
      def types = sealerate.values[DocType]
    }

    case class Address(
        AddressCode: String, //Input for GetTax only, not by address validation
        Line1: String,
        Line2: String,
        Line3: String,
        City: String,
        Region: String,
        PostalCode: String,
        Country: String,
        County: String, //Output for ValidateAddress only
        FipsCode: String, //Output for ValidateAddress only
        CarrierRoute: String, //Output for ValidateAddress only
        PostNet: String, //Output for ValidateAddress only
        AddressType: AddressType, //Output for ValidateAddress only
        Latitude: BigDecimal, //Input for GetTax only
        Longitude: BigDecimal, //Input for GetTax only
        TaxRegionId: String    //Input for GetTax only
    )

    case class TaxOverrideDef(
        TaxOverrideType: String, //Limited permitted values: TaxAmount, Exemption, TaxDate
        Reason: String,
        TaxAmount: String, //If included, must be valid decimal
        TaxDate: String
    )

    case class Line(
        LineNo: String, //Required
        DestinationCode: String, //Required
        OriginCode: String, //Required
        ItemCode: String, //Required
        Qty: BigDecimal, //Required
        Amount: BigDecimal, //Required
        TaxCode: String, //Best practice
        CustomerUsageType: String,
        Description: String, //Best Practice
        Discounted: Boolean,
        TaxIncluded: Boolean,
        Ref1: String,
        Ref2: String,
        BusinessIdentificationNo: String,
        TaxOverride: TaxOverrideDef
    )

    case class GetTaxes(
        //Required for tax calculation
        DocDate: Instant, //Must be valid YYYY-MM-DD format
        CustomerCode: String,
        Addresses: Seq[Address],
        Lines: Seq[Line],
        //Best Practice for tax calculation
        DocCode: String,
        DocType: DocType,
        CompanyCode: String,
        Commit: Boolean,
        DetailLevel: DetailLevel,
        Client: String,
        //Use where appropriate to the situation
        CustomerUsageType: String,
        ExemptionNo: String,
        Discount: BigDecimal,
        TaxOverride: TaxOverrideDef,
        BusinessIdentificationNo: String,
        //Optional
        PurchaseOrderNo: String,
        PaymentDate: String,
        ReferenceCode: String,
        PosLaneCode: String,
        CurrencyCode: String
    )
  }

  object Responses {
    sealed trait SeverityLevel
    case object Success   extends SeverityLevel
    case object Warning   extends SeverityLevel
    case object Error     extends SeverityLevel
    case object Exception extends SeverityLevel

    object SeverityLevel extends ADT[SeverityLevel] {
      def types = sealerate.values[SeverityLevel]
    }

    val severityLevelFormatter = SeverityLevel.jsonFormats

    case class Message(
        Summary: String,
        Details: Option[String],
        RefersTo: Option[String],
        Severity: String,
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
        DocCode: Option[String] = None,
        DocDate: Option[Instant] = None,
        Timestamp: Option[Instant] = None,
        TotalAmount: Option[Double] = None,
        TotalDiscount: Option[Double] = None,
        TotalExemption: Option[Double] = None,
        TotalTaxable: Option[Double] = None,
        TotalTax: Option[Double] = None,
        TotalTaxCalculated: Option[Double] = None,
        TaxDate: Option[Instant] = None,
        TaxLines: Option[Seq[TaxLine]] = None,
        TaxSummary: Option[Seq[TaxLine]] = None,
        TaxAddresses: Option[Seq[TaxAddress]] = None,
        ResultCode: Option[String] = None,
        Messages: Seq[Message] = Seq()
    )

    case class SimpleErrorMessage(
        ResultCode: Option[String],
        Messages: Seq[Message]
    )

  }
}

class Avalara()(implicit as: ActorSystem, am: ActorMaterializer) extends AvalaraApi {
  private def getConfig(): (String, String, String, String) = {
    val url     = config.getString("avalara.url")
    val account = config.getString("avalara.account")
    val license = config.getString("avalara.license")
    val profile = config.getString("avalara.profile")
    (url, account, license, profile)
  }

  implicit val formats: Formats = org.json4s.DefaultFormats + time.JavaTimeJson4sSerializer.jsonFormat + Money.jsonFormat + Avalara.Responses.SeverityLevel.jsonFormat

  implicit def responseUnmarshaller[T: Manifest]: FromResponseUnmarshaller[T] = {
    new Unmarshaller[HttpResponse, T] {
      override def apply(resp: HttpResponse)(implicit ec: EC, am: Materializer): Future[T] = {
        resp.entity
          .withContentType(ContentTypes.`application/json`)
          .toStrict(1.second)
          .map(_.data)
          .map(_.decodeString("utf-8"))
          .map(json ⇒ { println(s"Deserialized to: $json"); json })
          .map(
              json ⇒ { println(s"Received $json"); val a = parse(json).extract[T]; println(a); a })
      }
    }
  }

  override def estimateTax: Result[Unit] = Result.unit

  override def getTax()(implicit ec: EC): Result[Unit] = {
    val (url, account, license, profile) = getConfig()
    val connectionFlow: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]] =
      Http().outgoingConnectionHttps(url)
    val headers: Seq[HttpHeader] = Seq(Authorization(BasicHttpCredentials(account, license)))

//    val responseFuture: Future[HttpResponse] = Source
//      .single(HttpRequest(uri = "/1.0/tax/get", method = HttpMethods.POST, headers = headers))
//      .via(connectionFlow)
//      .runWith(Sink.head)

    val result: Future[Option[Avalara.Responses.GetTaxes]] = Source
      .single(HttpRequest(uri = "/1.0/tax/get", method = HttpMethods.POST, headers = headers))
      .via(connectionFlow)
      .mapAsync(1)(response ⇒ Unmarshal(response).to[Option[Avalara.Responses.GetTaxes]])
      .runWith(Sink.head)

//    val result: Future[Avalara.Responses.GetTaxes] =
//      responseFuture.flatMap(response ⇒ Unmarshal(response).to[Avalara.Responses.GetTaxes])

    val itWillBe = result.map {
      case Some(res) ⇒ {
        println(s"Result: $res")
        Result.unit
      }
      case None ⇒ {
        println("No result")
        Result.unit
      }
    } orE {
      println("We are doomed by failre")
      Result.left(UnableToMatchResponse.single)
    }
    itWillBe
  }
}
