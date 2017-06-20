package phoenix.models.inventory

import java.time.Instant

import cats.implicits._
import com.github.tminglei.slickpg._
import core.db.ExPostgresDriver.api._
import core.db._
import core.failures.Failures
import objectframework.models._
import phoenix.failures.ArchiveFailures.{LinkInactiveSkuFailure, SkuIsPresentInCarts}
import phoenix.models.cord.lineitems.CartLineItems
import phoenix.utils.JsonFormatters
import shapeless._

object Sku {
  val kind         = "sku"
  val skuCodeRegex = """([a-zA-Z0-9-_]*)""".r
}

/**
  * A Sku represents the latest version of Stock Keeping Unit.
  * This data structure stores a pointer to a commit of a version of a sku in
  * the object context referenced. The same Sku can have a different version
  * in a different context.
  */
case class Sku(id: Int = 0,
               scope: LTree,
               code: String,
               contextId: Int,
               shadowId: Int,
               formId: Int,
               commitId: Int,
               updatedAt: Instant = Instant.now,
               createdAt: Instant = Instant.now,
               archivedAt: Option[Instant] = None)
    extends FoxModel[Sku]
    with ObjectHead[Sku] {

  def withNewShadowAndCommit(shadowId: Int, commitId: Int): Sku =
    this.copy(shadowId = shadowId, commitId = commitId)

  def mustNotBeArchived[T](target: T, targetId: Any): Either[Failures, Sku] =
    if (archivedAt.isEmpty) Either.right(this)
    else Either.left(LinkInactiveSkuFailure(target, targetId, code).single)

  def mustNotBePresentInCarts(implicit ec: EC, db: DB): DbResultT[Unit] =
    for {
      inCartCount ← * <~ CartLineItems.filter(_.skuId === id).size.result
      _           ← * <~ failIf(inCartCount > 0, SkuIsPresentInCarts(code))
    } yield {}

}

class Skus(tag: Tag) extends ObjectHeads[Sku](tag, "skus") {

  def code = column[String]("code")

  def * =
    (id, scope, code, contextId, shadowId, formId, commitId, updatedAt, createdAt, archivedAt) <> ((Sku.apply _).tupled, Sku.unapply)
}

object Skus
    extends FoxTableQuery[Sku, Skus](new Skus(_))
    with ReturningId[Sku, Skus]
    with SearchByCode[Sku, Skus] {

  val returningLens: Lens[Sku, Int] = lens[Sku].id

  implicit val formats = JsonFormatters.phoenixFormats

  def filterByContext(contextId: Int): QuerySeq =
    filter(_.contextId === contextId)
  def filterByContextAndCode(contextId: Int, code: String): QuerySeq =
    filter(_.contextId === contextId).filter(_.code.toLowerCase === code.toLowerCase)
  def filterByCode(code: String): QuerySeq =
    filter(_.code.toLowerCase === code.toLowerCase)
  def findOneByCode(code: String): DBIO[Option[Sku]] =
    filter(_.code.toLowerCase === code.toLowerCase).one
}
