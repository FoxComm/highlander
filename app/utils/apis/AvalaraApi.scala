package utils.apis

import java.net.URLEncoder
import java.time.Instant

import scala.collection.immutable.{Seq ⇒ ImmutableSeq}
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
import org.json4s.ext._
import com.pellucid.sealerate
import failures.AvalaraFailures._
import models.cord._
import models.cord.lineitems.CartLineItems.FindLineItemResult
import models.location._
import services.Result
import utils.{ADT, JsonFormatters, Money, time}
import utils.FoxConfig._
import utils.aliases.EC
import utils.apis.Avalara.PayloadBuilder
import utils.apis.Avalara.Responses._

trait AvalaraApi {

  def validateAddress(address: Address, region: Region, country: Country)(
      implicit ec: EC): Result[Unit]
  def getTaxForCart(cart: Cart,
                    lineItems: Seq[FindLineItemResult],
                    address: Address,
                    region: Region,
                    country: Country)(implicit ec: EC): Result[Int]
  def getTaxForOrder(cart: Cart,
                     lineItems: Seq[FindLineItemResult],
                     address: Address,
                     region: Region,
                     country: Country)(implicit ec: EC): Result[Int]
  def cancelTax(order: Order)(implicit ec: EC): Result[Unit]

}

object Avalara {
  trait AvalaraADT[W] extends ADT[W] {
    override def show(f: W): String = f.toString
  }

  sealed trait AddressType
  case object F extends AddressType //Firm or company address
  case object G extends AddressType //General Delivery address
  case object H extends AddressType //High-rise or business complex
  case object P extends AddressType //PO box address
  case object R extends AddressType //Rural route address
  case object S extends AddressType //Street or residential address, probably, we will mostly use it

  object AddressType extends AvalaraADT[AddressType] {
    def types = sealerate.values[AddressType]
  }

  sealed trait DetailLevel
  case object Tax        extends DetailLevel
  case object Document   extends DetailLevel
  case object Line       extends DetailLevel
  case object Diagnostic extends DetailLevel

  object DetailLevel extends AvalaraADT[DetailLevel] {
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

  object DocType extends AvalaraADT[DocType] {
    def types = sealerate.values[DocType]
  }

  sealed trait CancelCode
  case object Unspecified         extends CancelCode
  case object PostFailed          extends CancelCode
  case object DocDeleted          extends CancelCode
  case object DocVoided           extends CancelCode
  case object AdjustmentCancelled extends CancelCode

  object CancelCode extends AvalaraADT[CancelCode] {
    def types = sealerate.values[CancelCode]
  }

  case class AvalaraAddress(
      AddressCode: Option[String] = None, //Input for GetTax only, not by address validation
      Line1: String,
      Line2: Option[String] = None,
      Line3: Option[String] = None,
      City: String,
      Region: String,
      PostalCode: String,
      Country: String,
      County: Option[String] = None, //Output for ValidateAddress only
      FipsCode: Option[String] = None, //Output for ValidateAddress only
      CarrierRoute: Option[String] = None, //Output for ValidateAddress only
      PostNet: Option[String] = None, //Output for ValidateAddress only
      AddressType: Option[AddressType] = None, //Output for ValidateAddress only
      Latitude: Option[BigDecimal] = None, //Input for GetTax only
      Longitude: Option[BigDecimal] = None, //Input for GetTax only
      TaxRegionId: Option[String] = None    //Input for GetTax only
  ) {
    def toQuery(): String = {
      def p(string: String)          = URLEncoder.encode(string, "UTF-8")
      def po(string: Option[String]) = string.map(URLEncoder.encode(_, "UTF-8")).getOrElse("")
      s"Line1=${p(Line1)}&Line2=${po(Line2)}&Line3=${po(Line3)}&City=${p(City)}&Region=${p(Region)}&" +
      s"PostalCode=${p(PostalCode)}&Country=${p(Country)}"
    }
  }

  object PayloadBuilder {
    def buildAddress(address: Address, region: Region, country: Country)(
        implicit formats: Formats): AvalaraAddress = {
      AvalaraAddress(AddressCode = Some(address.id.toString),
                     Line1 = address.address1,
                     Line2 = address.address2,
                     City = address.city,
                     PostalCode = address.zip,
                     Region = region.abbrev.getOrElse(""),
                     Country = country.alpha2)
    }

    def buildLine(lineItem: FindLineItemResult, idx: Int, addressId: Int)(
        implicit formats: Formats): Requests.Line = {
      val (sku, form, shadow, otherShadow, cartItem) = lineItem
      val ref                                        = (shadow.attributes \ "salePrice" \ "ref").extract[String]
      val price                                      = (form.attributes \ ref \ "value").extract[Int] / 100
      Requests.Line(
          DestinationCode = addressId.toString,
          OriginCode = addressId.toString,
          LineNo = idx.toString,
          ItemCode = sku.code,
          Qty = BigDecimal(1),
          Amount = BigDecimal(price)
      )
    }

    def buildInvoice(cart: Cart,
                     lineItems: Seq[FindLineItemResult],
                     address: Address,
                     region: Region,
                     country: Country)(implicit formats: Formats): Requests.GetTaxes = {
      Requests.GetTaxes(
          CustomerCode = cart.customerId.toString,
          Addresses = Seq(buildAddress(address, region, country)),
          Lines = lineItems
            .zip(Stream.from(1))
            .map(zipped ⇒ buildLine(zipped._1, zipped._2, address.id)),
          DocCode = cart.referenceNumber,
          Commit = true,
          DocType = SalesInvoice
      )
    }

    def buildOrder(cart: Cart,
                   lineItems: Seq[FindLineItemResult],
                   address: Address,
                   region: Region,
                   country: Country)(implicit formats: Formats): Requests.GetTaxes = {
      Requests.GetTaxes(
          CustomerCode = cart.customerId.toString,
          Addresses = Seq(buildAddress(address, region, country)),
          Lines = lineItems
            .zip(Stream.from(1))
            .map(zipped ⇒ buildLine(zipped._1, zipped._2, address.id)),
          DocCode = cart.referenceNumber,
          DocType = SalesOrder
      )
    }

    def cancelOrder(cord: Order)(implicit formats: Formats): Requests.CancelTax = {
      Requests.CancelTax(DocCode = cord.referenceNumber)
    }
  }

  object Requests {

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
        TaxCode: Option[String] = None, //Best practice
        CustomerUsageType: Option[String] = None,
        Description: Option[String] = None, //Best Practice
        Discounted: Boolean = false,
        TaxIncluded: Boolean = false,
        Ref1: Option[String] = None,
        Ref2: Option[String] = None,
        BusinessIdentificationNo: Option[String] = None,
        TaxOverride: Option[TaxOverrideDef] = None
    )

    case class GetTaxes(
        //Required for tax calculation
        CustomerCode: String,
        Addresses: Seq[AvalaraAddress],
        Lines: Seq[Line],
        //Best Practice for tax calculation
        DocCode: String,
        Commit: Boolean = false,
        DocType: DocType
    )

    case class CancelTax(
        DocType: DocType = SalesInvoice,
        CancelCode: CancelCode = DocDeleted,
        DocCode: String
    )
  }

  object Responses {
    sealed trait SeverityLevel
    case object Success   extends SeverityLevel
    case object Warning   extends SeverityLevel
    case object Error     extends SeverityLevel
    case object Exception extends SeverityLevel

    object SeverityLevel extends AvalaraADT[SeverityLevel] {
      def types = sealerate.values[SeverityLevel]
    }

    trait AvalaraResponse {
      def ResultCode: SeverityLevel
      def hasError: Boolean = ResultCode == Error
      def Messages: Seq[Message]
      def collectMessages = Messages.map(_.Summary).mkString(", ")
    }

    case class Message(
        Summary: String,
        Details: Option[String],
        RefersTo: Option[String],
        Severity: SeverityLevel,
        Source: String
    )

    case class TaxAddress(
        Address: Option[String],
        AddressCode: Option[String],
        City: Option[String],
        Region: Option[String],
        Country: Option[String],
        PostalCode: Option[String],
        Latitude: Option[String],
        Longitude: Option[String],
        TaxRegionId: Option[String],
        JurisCode: Option[String]
    )

    case class TaxDetail(
        Rate: Option[String],
        Tax: Option[String],
        Taxable: Option[String],
        Country: Option[String],
        Region: Option[String],
        JurisType: Option[String],
        JurisName: Option[String],
        TaxName: Option[String]
    )

    case class TaxLine(
        LineNo: Option[String],
        TaxCode: Option[String],
        Taxability: Option[String],
        Taxable: Option[String],
        Rate: Option[String],
        Tax: Option[String],
        Discount: Option[String],
        TaxCalculated: Option[String],
        Exemption: Option[String],
        TaxDetails: Seq[TaxDetail],
        BoundaryLevel: Option[String]
    )

    case class GetTaxes(
        DocCode: Option[String],
        DocDate: Option[String],
        Timestamp: Option[String],
        TotalAmount: Option[String],
        TotalDiscount: Option[String],
        TotalExemption: Option[String],
        TotalTaxable: Option[String],
        TotalTax: Option[String],
        TotalTaxCalculated: Option[String],
        TaxDate: Option[String],
        TaxLines: Seq[TaxLine],
        TaxAddresses: Seq[TaxAddress],
        ResultCode: SeverityLevel,
        Messages: Seq[Message]
    ) extends AvalaraResponse

    case class AddressValidation(
        Address: Option[AvalaraAddress],
        ResultCode: SeverityLevel,
        Messages: Seq[Message]
    ) extends AvalaraResponse

    case class CancelTax(
        ResultCode: SeverityLevel,
        TransactionId: Option[String],
        DocId: Option[String],
        Messages: Seq[Message]
    ) extends AvalaraResponse
  }
}

object AvalaraAdapter {
  def apply(url: String, account: String, license: String, profile: String)(
      implicit as: ActorSystem,
      am: ActorMaterializer) = {
    new Avalara(url, account, license, profile)
  }
}

class Avalara(url: String, account: String, license: String, profile: String)(
    implicit as: ActorSystem,
    am: ActorMaterializer)
    extends AvalaraApi {

  implicit val formats: Formats = JsonFormatters.avalaraFormats

  implicit def responseUnmarshaller[T: Manifest]: FromResponseUnmarshaller[T] = {
    new Unmarshaller[HttpResponse, T] {
      override def apply(resp: HttpResponse)(implicit ec: EC, am: Materializer): Future[T] = {
        resp.entity
          .withContentType(ContentTypes.`application/json`)
          .toStrict(1.second)
          .map(_.data)
          .map(_.decodeString("utf-8"))
          .map(json ⇒ { println(json); json })
          .map(json ⇒ parse(json).extract[T])
      }
    }
  }

  override def validateAddress(address: Address, region: Region, country: Country)(
      implicit ec: EC): Result[Unit] = {
    val connectionFlow: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]] =
      Http().outgoingConnectionHttps(url)

    val headers: ImmutableSeq[HttpHeader] = ImmutableSeq(
        Authorization(BasicHttpCredentials(account, license)))
    val payload = PayloadBuilder.buildAddress(address, region, country)

    val result: Future[Avalara.Responses.AddressValidation] = Source
      .single(HttpRequest(uri = s"/1.0/address/validate?${payload.toQuery}",
                          method = HttpMethods.GET,
                          headers = headers))
      .via(connectionFlow)
      .mapAsync(1)(response ⇒ Unmarshal(response).to[Avalara.Responses.AddressValidation])
      .runWith(Sink.head)

    result.flatMap {
      case response ⇒
        if (!response.hasError) {
          Result.unit
        } else {
          val message = response.collectMessages
          Result.failure(AddressValidationFailure(message))
        }
    }.recoverWith {
      case err: Throwable ⇒ failureHandler(err)
    }
  }

  override def getTaxForCart(cart: Cart,
                             lineItems: Seq[FindLineItemResult],
                             address: Address,
                             region: Region,
                             country: Country)(implicit ec: EC): Result[Int] = {
    val payload = PayloadBuilder.buildOrder(cart, lineItems, address, region, country)
    println(write(payload))
    getTax(payload)
  }

  override def getTaxForOrder(cart: Cart,
                              lineItems: Seq[FindLineItemResult],
                              address: Address,
                              region: Region,
                              country: Country)(implicit ec: EC): Result[Int] = {
    val payload = PayloadBuilder.buildInvoice(cart, lineItems, address, region, country)
    println(write(payload))
    getTax(payload)
  }

  override def cancelTax(order: Order)(implicit ec: EC): Result[Unit] = {
    val connectionFlow: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]] =
      Http().outgoingConnectionHttps(url)
    val headers: ImmutableSeq[HttpHeader] = ImmutableSeq(
        Authorization(BasicHttpCredentials(account, license)))

    val payload = PayloadBuilder.cancelOrder(order)

    val result: Future[Avalara.Responses.CancelTax] = Source
      .single(
          HttpRequest(uri = "/1.0/tax/cancel",
                      method = HttpMethods.POST,
                      headers = headers,
                      entity = HttpEntity(write(payload))))
      .via(connectionFlow)
      .mapAsync(1)(response ⇒ Unmarshal(response).to[Avalara.Responses.CancelTax])
      .runWith(Sink.head)

    result.flatMap { res ⇒
      if (!res.hasError) {
        Result.unit
      } else {
        val message = res.collectMessages
        Result.failure(TaxCancellationFailure(message))
      }
    }.recoverWith {
      case err: Throwable ⇒ failureHandler(err)
    }
  }

  private def getTax(payload: Avalara.Requests.GetTaxes)(implicit ec: EC): Result[Int] = {
    val connectionFlow: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]] =
      Http().outgoingConnectionHttps(url)
    val headers: ImmutableSeq[HttpHeader] = ImmutableSeq(
        Authorization(BasicHttpCredentials(account, license)))

    val result: Future[Avalara.Responses.GetTaxes] = Source
      .single(
          HttpRequest(uri = "/1.0/tax/get",
                      method = HttpMethods.POST,
                      headers = headers,
                      entity = HttpEntity(write(payload))))
      .via(connectionFlow)
      .mapAsync(1)(response ⇒ Unmarshal(response).to[Avalara.Responses.GetTaxes])
      .runWith(Sink.head)

    result.flatMap { res ⇒
      if (!res.hasError) {
        println("no errors")
        val taxCalculated = (res.TotalTaxCalculated.getOrElse("0").toDouble * 100).toInt
        Result.good(taxCalculated)
      } else {
        val message = res.collectMessages
        if (res.Messages.exists { m ⇒
              m.RefersTo.contains("Addresses")
            }) {
          Result.failure(AddressValidationFailure(message))
        } else {
          Result.failure(TaxApplicationFailure(message))
        }
      }
    }.recoverWith {
      case err: Throwable ⇒ failureHandler(err)
    }
  }

  private def failureHandler(failure: Throwable) = {
    Result.left(UnableToMatchResponse.single)
  }
}
