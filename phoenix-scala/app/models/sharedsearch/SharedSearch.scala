package models.sharedsearch

import java.time.Instant

import com.github.tminglei.slickpg.LTree
import com.pellucid.sealerate
import models.account._
import models.sharedsearch.SharedSearch._
import payloads.SharedSearchPayloads.SharedSearchPayload
import shapeless._
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import utils.aliases._
import utils.db.ExPostgresDriver.api._
import utils.db._
import utils.{ADT, JsonFormatters}

case class SharedSearch(id: Int = 0,
                        code: String = "",
                        accessScope: LTree,
                        title: String,
                        query: Json,
                        rawQuery: Json,
                        scope: SharedSearch.Scope = SharedSearch.CustomersScope,
                        storeAdminId: Int,
                        isSystem: Boolean = false,
                        createdAt: Instant = Instant.now,
                        deletedAt: Option[Instant] = None)
    extends FoxModel[SharedSearch] {}

object SharedSearch {
  sealed trait Scope
  case object CustomersScope      extends Scope
  case object CustomerGroupsScope extends Scope
  case object OrdersScope         extends Scope
  case object GiftCardsScope      extends Scope
  case object ProductsScope       extends Scope
  case object InventoryScope      extends Scope
  case object StoreAdminsScope    extends Scope
  case object PromotionsScope     extends Scope
  case object CouponsScope        extends Scope
  case object CouponCodesScope    extends Scope
  case object SkusScope           extends Scope
  case object CartsScope          extends Scope

  object Scope extends ADT[Scope] {
    def types = sealerate.values[Scope]
  }

  val sharedSearchRegex = """([a-z0-9]*)""".r

  def byAdmin(admin: User, payload: SharedSearchPayload, scope: LTree): SharedSearch =
    SharedSearch(
        title = payload.title,
        query = payload.query,
        rawQuery = payload.rawQuery,
        storeAdminId = admin.accountId,
        scope = payload.scope,
        accessScope = scope
    )

  implicit val scopeColumnType: JdbcType[Scope] with BaseTypedType[Scope] = Scope.slickColumn
}

class SharedSearches(tag: Tag) extends FoxTable[SharedSearch](tag, "shared_searches") {
  def id           = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def code         = column[String]("code")
  def accessScope  = column[LTree]("access_scope")
  def title        = column[String]("title")
  def query        = column[Json]("query")
  def rawQuery     = column[Json]("raw_query")
  def scope        = column[SharedSearch.Scope]("scope")
  def storeAdminId = column[Int]("store_admin_id")
  def isSystem     = column[Boolean]("is_system")
  def createdAt    = column[Instant]("created_at")
  def deletedAt    = column[Option[Instant]]("deleted_at")

  def * =
    (id,
     code,
     accessScope,
     title,
     query,
     rawQuery,
     scope,
     storeAdminId,
     isSystem,
     createdAt,
     deletedAt) <> ((SharedSearch.apply _).tupled, SharedSearch.unapply)
}

object SharedSearches
    extends FoxTableQuery[SharedSearch, SharedSearches](new SharedSearches(_))
    with ReturningIdAndString[SharedSearch, SharedSearches]
    with SearchByCode[SharedSearch, SharedSearches] {

  implicit val formats = JsonFormatters.phoenixFormats

  import scope._

  override val returningQuery = map { s ⇒
    (s.id, s.code)
  }

  def findOneByCode(code: String): DBIO[Option[SharedSearch]] =
    filter(_.code === code).one

  def findActiveByCode(code: String): DBIO[Option[SharedSearch]] =
    filter(_.code === code).notDeleted.one

  def findActiveByCodeAndAdmin(code: String, adminId: Int): DBIO[Option[SharedSearch]] =
    filter(_.code === code).filter(_.storeAdminId === adminId).notDeleted.one

  def findActiveByScopeAndAdmin(scope: SharedSearch.Scope, adminId: Int): QuerySeq =
    byAdmin(adminId).filter(_.scope === scope).notDeleted

  def byAdmin(adminId: Int): QuerySeq = filter(_.storeAdminId === adminId)

  object scope {
    implicit class SharedSearchQuerySeqConversions(q: QuerySeq) {
      def notDeleted: QuerySeq =
        q.filterNot(_.deletedAt.isDefined)
    }
  }

  private val rootLens                                 = lens[SharedSearch]
  val returningLens: Lens[SharedSearch, (Int, String)] = rootLens.id ~ rootLens.code
}
