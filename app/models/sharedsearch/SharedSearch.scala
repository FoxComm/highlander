package models.sharedsearch

import java.time.Instant

import com.pellucid.sealerate
import models.{StoreAdmin, javaTimeSlickMapper}
import models.sharedsearch.SharedSearch._
import monocle.macros.GenLens
import org.json4s.JsonAST.JValue
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import utils.ExPostgresDriver.api._
import utils.Slick._
import utils.Slick.implicits._
import utils.table.SearchByCode
import utils.{ADT, GenericTable, JsonFormatters, ModelWithIdParameter, TableQueryWithId}
import utils.aliases._

case class SharedSearch(id: Int = 0, code: String = "", title: String, query: JValue,
  scope: Scope = CustomersScope, storeAdminId: Int, createdAt: Instant = Instant.now, deletedAt: Option[Instant] = None)
  extends ModelWithIdParameter[SharedSearch] {

}

object SharedSearch {
  sealed trait Scope
  case object CustomersScope extends Scope
  case object OrdersScope extends Scope
  case object GiftCardsScope extends Scope
  case object ProductsScope extends Scope
  case object InventoryScope extends Scope
  case object StoreAdminsScope extends Scope
  case object PromotionsScope extends Scope
  case object CouponsScope extends Scope
  case object CouponCodesScope extends Scope

  object Scope extends ADT[Scope] {
    def types = sealerate.values[Scope]
  }

  val sharedSearchRegex = """([a-z0-9]*)""".r

  def byAdmin(admin: StoreAdmin, payload: payloads.SharedSearchPayload): SharedSearch =
    SharedSearch(
      title = payload.title,
      query = payload.query,
      storeAdminId = admin.id,
      scope = payload.scope
    )

  implicit val scopeColumnType: JdbcType[Scope] with BaseTypedType[Scope] = Scope.slickColumn
}

class SharedSearches(tag: Tag) extends GenericTable.TableWithId[SharedSearch](tag, "shared_searches")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def code = column[String]("code")
  def title = column[String]("title")
  def query = column[JValue]("query")
  def scope = column[Scope]("scope")
  def storeAdminId = column[Int]("store_admin_id")
  def createdAt = column[Instant]("created_at")
  def deletedAt = column[Option[Instant]]("deleted_at")

  def * = (id, code, title, query, scope, storeAdminId, createdAt, deletedAt) <> ((SharedSearch.apply _).tupled,
    SharedSearch.unapply)
}

object SharedSearches extends TableQueryWithId[SharedSearch, SharedSearches](
  idLens = GenLens[SharedSearch](_.id)
)(new SharedSearches(_))
  with SearchByCode[SharedSearch, SharedSearches] {

  implicit val formats = JsonFormatters.phoenixFormats

  import scope._

  val returningIdAndCode = this.returning(map { s ⇒ (s.id, s.code) })

  def returningAction(ret: (Int, String))(search: SharedSearch): SharedSearch = ret match {
    case (id, code) ⇒ search.copy(id = id, code = code)
  }

  override def create[R](search: SharedSearch, returning: Returning[R], action: R ⇒ SharedSearch ⇒ SharedSearch)
    (implicit ec: EC): DbResult[SharedSearch] = super.create(search, returningIdAndCode, returningAction)

  def findOneByCode(code: String): DBIO[Option[SharedSearch]] =
    filter(_.code === code).one

  def findActiveByCode(code: String): DBIO[Option[SharedSearch]] =
    filter(_.code === code).notDeleted.one

  def findActiveByCodeAndAdmin(code: String, adminId: Int): DBIO[Option[SharedSearch]] =
    filter(_.code === code).filter(_.storeAdminId === adminId).notDeleted.one

  def findActiveByScopeAndAdmin(scope: Scope, adminId: Int): QuerySeq =
    byAdmin(adminId).filter(_.scope === scope).notDeleted

  def byAdmin(adminId: Int): QuerySeq = filter(_.storeAdminId === adminId)

  object scope {
    implicit class SharedSearchQuerySeqConversions(q: QuerySeq) {
      def notDeleted: QuerySeq =
        q.filterNot(_.deletedAt.isDefined)
    }
  }
}
