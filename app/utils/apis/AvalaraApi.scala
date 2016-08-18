package utils.apis

import java.time.Instant

import com.pellucid.sealerate
import services.Result
import utils.ADT
import utils.FoxConfig._

trait AvalaraApi {

  def estimateTax: Result[Unit]
  def getTax: Result[Unit]

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

class Avalara extends AvalaraApi {
  private def getConfig(): (Option[String], Option[String], Option[String], Option[String]) = {
    val url     = config.getOptString("avalara.url")
    val account = config.getOptString("avalara.account")
    val licence = config.getOptString("avalara.licence")
    val profile = config.getOptString("avalara.profile")
    (url, account, licence, profile)
  }

  override def estimateTax: Result[Unit] = Result.unit

  override def getTax: Result[Unit] = Result.unit
}
