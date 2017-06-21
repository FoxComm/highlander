package phoenix.payloads

import com.pellucid.sealerate
import com.sksamuel.elastic4s.SortDefinition
import java.nio.charset.StandardCharsets
import java.time.Instant

import org.elasticsearch.common.bytes.BytesArray
import org.elasticsearch.common.xcontent.{ToXContent, XContentBuilder}
import org.elasticsearch.search.sort.{SortBuilder, SortOrder}
import org.json4s.{CustomSerializer, Formats}
import org.json4s.JsonAST._
import org.json4s.jackson.JsonMethods._
import phoenix.models.cord.CordPaymentState
import phoenix.models.payment.{ExternalCharge, InStorePaymentStates}
import phoenix.models.payment.creditcard.CreditCardCharge
import phoenix.services.CordQueries
import phoenix.utils.{ADT, ADTTypeHints, JsonFormatters}
import core.utils.Strings._

object EntityExportPayloads {

  /** Denotes entity that can be exported.
    *
    * Note that `dynamicCalculations` allows for exporting fields,
    * that aren't in ElasticSearch directly,
    * but are calculated based on other fields values.
    * Order in list matters, as if there are more than one `FieldCalculation` that reacts to the same field name,
    * only the last one will be taken into account.
    */
  sealed abstract class ExportableEntity(dynamicCalculations: List[FieldCalculation]) { this: Product ⇒
    def this(dynamicCalculations: FieldCalculation*) = this(dynamicCalculations.toList)

    lazy val (extraFields, calculateFields) =
      dynamicCalculations.foldLeft(List.empty[String] → PartialFunction.empty[(String, JObject), String]) {
        case ((accFields, accCalc), fc) ⇒
          (fc.extraFields ::: accFields, fc.calculate.orElse(accCalc))
      }

    def entity: String = productPrefix.underscore

    def searchView: String = s"${entity}_search_view"
  }

  sealed trait FieldCalculation {
    def extraFields: List[String]

    def calculate: PartialFunction[(String, JObject), String]
  }
  object FieldCalculation {
    implicit def formats: Formats = JsonFormatters.phoenixFormats

    object State extends FieldCalculation {
      def extraFields: List[String] = List("activeFrom", "activeTo", "archivedAt")

      lazy val calculate: PartialFunction[(String, JObject), String] = {
        val active   = "\"Active\""
        val inactive = "\"Inactive\""

        {
          case ("state", jobj) ⇒
            val doc = jobj.obj.toMap

            val activeFrom = doc.get("activeFrom").flatMap(_.extractOpt[Instant])
            val activeTo   = doc.get("activeTo").flatMap(_.extractOpt[Instant])
            val archivedAt = doc.get("archivedAt").flatMap(_.extractOpt[Instant])
            val now        = Instant.now()

            (archivedAt, activeFrom, activeTo) match {
              case (None, Some(from), Some(to)) if from.isBefore(now) && to.isAfter(now) ⇒ active
              case (None, Some(from), None) if from.isBefore(now)                        ⇒ active
              case _                                                                     ⇒ inactive
            }
        }
      }
    }

    object PaymentState extends FieldCalculation {
      def extraFields: List[String] = List("payments")

      // todo add apple pay here ?  (this piece was merge in on rebase to master) @aafa
      lazy val calculate: PartialFunction[(String, JObject), String] = {
        def getState(jv: JValue): Option[CordPaymentState.State] =
          (jv \ "creditCardState")
            .extractOpt[String]
            .flatMap(ExternalCharge.State.read)
            .map(CordPaymentState.fromExternalState) orElse
            (jv \ "giftCardState")
              .extractOpt[String]
              .flatMap(InStorePaymentStates.State.read)
              .map(CordPaymentState.fromInStoreState) orElse
            (jv \ "storeCreditState")
              .extractOpt[String]
              .flatMap(InStorePaymentStates.State.read)
              .map(CordPaymentState.fromInStoreState)

        {
          case ("payment.state", jobj) ⇒
            val payments = jobj.obj.toMap
              .get("payments")
              .flatMap(_.extractOpt[JArray])
              .getOrElse(JArray(Nil))
              .arr
            val cordPayments = payments.flatMap(getState)

            CordQueries.foldPaymentStates(cordPayments, payments.size).toString.prettify.quote('"')
        }
      }
    }
  }

  object ExportableEntity extends ADT[ExportableEntity] {
    case object Carts          extends ExportableEntity
    case object CouponCodes    extends ExportableEntity
    case object Coupons        extends ExportableEntity(FieldCalculation.State)
    case object CustomerGroups extends ExportableEntity
    case object CustomerItems extends ExportableEntity {
      override def searchView: String = s"${entity}_view"
    }
    case object Customers extends ExportableEntity
    case object GiftCards extends ExportableEntity
    case object GiftCardTransactions extends ExportableEntity {
      override def searchView: String = s"${entity}_view"
    }
    case object Inventory             extends ExportableEntity
    case object InventoryTransactions extends ExportableEntity
    case object Notes                 extends ExportableEntity
    case object Orders                extends ExportableEntity(FieldCalculation.PaymentState)
    case object Products              extends ExportableEntity(FieldCalculation.State)
    case object Promotions            extends ExportableEntity(FieldCalculation.State)
    case object Skus extends ExportableEntity {
      override def searchView: String = s"sku_search_view"
    }
    case object StoreAdmins             extends ExportableEntity
    case object StoreCredits            extends ExportableEntity
    case object StoreCreditTransactions extends ExportableEntity
    case object Taxonomies              extends ExportableEntity(FieldCalculation.State)
    case object Taxons                  extends ExportableEntity(FieldCalculation.State)

    def types: Set[ExportableEntity] = sealerate.collect[ExportableEntity]
  }

  case class ExportField(name: String, displayName: String)

  sealed trait ExportEntity {
    def description: Option[String]

    def fields: List[ExportField]
  }
  object ExportEntity {
    def typeHints =
      ADTTypeHints(
        Map(
          Type.Ids   → classOf[ByIDs],
          Type.Query → classOf[BySearchQuery]
        ))

    sealed trait Type extends Product with Serializable
    implicit object Type extends ADT[Type] {
      case object Ids   extends Type
      case object Query extends Type

      def types: Set[Type] = sealerate.values[Type]
    }

    case class ByIDs(description: Option[String], fields: List[ExportField], ids: List[Long])
        extends ExportEntity
    case class BySearchQuery(description: Option[String],
                             fields: List[ExportField],
                             query: JObject,
                             sort: Option[List[RawSortDefinition]])
        extends ExportEntity
  }

  case class RawSortDefinition(field: String, json: JObject) extends SortDefinition {
    lazy val builder: SortBuilder = new SortBuilder {
      private[this] lazy val bytes = new BytesArray(compact(render(json)).getBytes(StandardCharsets.UTF_8))

      def missing(missing: Any): SortBuilder = this // no need to support this operation

      def order(order: SortOrder): SortBuilder = this // no need to support this operation

      def toXContent(builder: XContentBuilder, params: ToXContent.Params): XContentBuilder =
        builder.rawField(field, bytes)
    }
  }
  object RawSortDefinition {
    val jsonFormat = new CustomSerializer[RawSortDefinition](_ ⇒
      ({
        case JString(field) ⇒ RawSortDefinition(field, JObject())
        case JObject((field, order @ JString(_)) :: Nil) ⇒
          RawSortDefinition(field, JObject("order" → order))
        case JObject((field, options @ JObject(_)) :: Nil) ⇒ RawSortDefinition(field, options)
      }, {
        case RawSortDefinition(field, JObject(Nil)) ⇒ JString(field)
        case RawSortDefinition(field, options)      ⇒ JObject(field → options)
      }))
  }
}
