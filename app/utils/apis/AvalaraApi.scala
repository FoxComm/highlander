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
import models.cord.{Cart, OrderShippingAddresses}
import models.cord.lineitems.CartLineItems.FindLineItemResult
import models.location._
import services.Result
import utils.{ADT, Money, time}
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
                    country: Country)(implicit ec: EC): Result[Unit]
  def getTaxForOrder(cart: Cart,
                     lineItems: Seq[FindLineItemResult],
                     address: Address,
                     region: Region,
                     country: Country)(implicit ec: EC): Result[Unit]

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

  object AddressType extends AvalaraADT[AddressType] {
    def types = sealerate.values[AddressType]
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
    def buildAddress(address: Address, region: Region, country: Country): AvalaraAddress = {
      AvalaraAddress(AddressCode = Some(address.id.toString),
                     Line1 = address.address1,
                     Line2 = address.address2,
                     City = address.city,
                     PostalCode = address.zip,
                     Region = region.abbrev.getOrElse(""),
                     Country = country.alpha2)
    }

    def buildLine(lineItem: FindLineItemResult, idx: Int, addressId: Int): Requests.Line = {
      val (sku, form, shadow, otherShadow, cartItem) = lineItem;
      Requests.Line(
          DestinationCode = addressId.toString,
          OriginCode = addressId.toString,
          LineNo = idx.toString,
          ItemCode = sku.code,
          Qty = BigDecimal(1),
          Amount = BigDecimal(10)
      )
    }

    def buildInvoice(cart: Cart,
                     lineItems: Seq[FindLineItemResult],
                     address: Address,
                     region: Region,
                     country: Country): Requests.GetTaxes = {
      Requests.GetTaxes(
          CustomerCode = cart.customerId.toString,
          Addresses = Seq(buildAddress(address, region, country)),
          Lines = lineItems.zipWithIndex.map(zipped ⇒ buildLine(zipped._1, zipped._2, address.id)),
          DocCode = cart.referenceNumber,
          Commit = true
      )
    }

    def buildOrder(cart: Cart,
                   lineItems: Seq[FindLineItemResult],
                   address: Address,
                   region: Region,
                   country: Country): Requests.GetTaxes = {
      Requests.GetTaxes(
          CustomerCode = cart.customerId.toString,
          Addresses = Seq(buildAddress(address, region, country)),
          Lines = lineItems.zipWithIndex.map(zipped ⇒ buildLine(zipped._1, zipped._2, address.id)),
          DocCode = cart.referenceNumber
      )
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
        Commit: Boolean = false
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

    val severityLevelFormatter = SeverityLevel.jsonFormats

    case class Message(
        Summary: String,
        Details: Option[String],
        RefersTo: Option[String],
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
        ResultCode: Option[String],
        Messages: Seq[Message]
    )

    case class AddressValidation(
        Address: Option[AvalaraAddress],
        ResultCode: SeverityLevel,
        Messages: Seq[Message]
    )

    case class SimpleErrorMessage(
        ResultCode: Option[String],
        Messages: Seq[Message]
    )
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

  implicit val formats: Formats = org.json4s.DefaultFormats + time.JavaTimeJson4sSerializer.jsonFormat + Money.jsonFormat + Avalara.Responses.SeverityLevel.jsonFormat

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

    def failureHandler(failure: Throwable) = {
      println(s"We are doomed by failre $failure")
      Result.left(UnableToMatchResponse.single)
    }

    result.flatMap {
      case response ⇒
        if (response.Address.isDefined || response.ResultCode == Success) {
          println("success")
          Result.unit
        } else {
          val message = response.Messages.map(_.Summary).mkString(", ")
          println(s"Error: $message")
//          Result.failure(AddressValidationFailure(message))
          Result.unit
        }
    }.recoverWith {
      case err: Throwable ⇒ failureHandler(err)
    }
  }

  override def getTaxForCart(cart: Cart,
                             lineItems: Seq[FindLineItemResult],
                             address: Address,
                             region: Region,
                             country: Country)(implicit ec: EC): Result[Unit] = {
    println("getting taxes for cart")
    val payload = PayloadBuilder.buildOrder(cart, lineItems, address, region, country)
    println(payload)
    println(write(payload))
    getTax(payload)
  }

  override def getTaxForOrder(cart: Cart,
                              lineItems: Seq[FindLineItemResult],
                              address: Address,
                              region: Region,
                              country: Country)(implicit ec: EC): Result[Unit] = {
    val payload = PayloadBuilder.buildInvoice(cart, lineItems, address, region, country)
    println(payload)
    println(write(payload))
    getTax(payload)
  }

  private def getTax(payload: Avalara.Requests.GetTaxes)(implicit ec: EC): Result[Unit] = {
    val connectionFlow: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]] =
      Http().outgoingConnectionHttps(url)
    val headers: ImmutableSeq[HttpHeader] = ImmutableSeq(
        Authorization(BasicHttpCredentials(account, license)))

    println("building request")
    val result: Future[Avalara.Responses.GetTaxes] = Source
      .single(
          HttpRequest(uri = "/1.0/tax/get",
                      method = HttpMethods.POST,
                      headers = headers,
                      entity = HttpEntity(write(payload))))
      .via(connectionFlow)
      .mapAsync(1)(response ⇒ Unmarshal(response).to[Avalara.Responses.GetTaxes])
      .runWith(Sink.head)

    def failureHandler(failure: Throwable) = {
      println(s"We are doomed by failre $failure")
      Result.left(UnableToMatchResponse.single)
    }

    result.flatMap {
      case res: GetTaxes ⇒ {
        println(s"Result: $res")
        Result.unit
      }
      case _ ⇒ {
        println("No result")
        Result.unit
      }
    }.recoverWith {
      case err: Throwable ⇒ failureHandler(err)
    }
  }
}
