package payloads

import com.pellucid.sealerate
import utils.Strings._
import utils.aliases.Json
import utils.{ADT, ADTTypeHints}

object ExportEntityPayloads {
  sealed trait ExportableEntity {
    this: Product ⇒
    def entity: String = productPrefix.underscore

    def searchView: String = s"${entity}_search_view"
  }
  object ExportableEntity extends ADT[ExportableEntity] {
    case object Carts          extends ExportableEntity
    case object CouponCodes    extends ExportableEntity
    case object Coupons        extends ExportableEntity
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
    case object Orders                extends ExportableEntity
    case object Products              extends ExportableEntity
    case object Promotions            extends ExportableEntity
    case object Skus extends ExportableEntity {
      override def searchView: String = s"sku_search_view"
    }
    case object StoreAdmins             extends ExportableEntity
    case object StoreCredits            extends ExportableEntity
    case object StoreCreditTransactions extends ExportableEntity
    case object Taxonomies              extends ExportableEntity
    case object Taxons                  extends ExportableEntity

    def types: Set[ExportableEntity] = sealerate.values[ExportableEntity]
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
    case class BySearchQuery(description: Option[String], fields: List[ExportField], query: Json)
        extends ExportEntity
  }
}
