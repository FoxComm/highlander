package models.inventory

import java.time.Instant

import cats.data.Xor
import failures.ArchiveFailures.{LinkArchivedSkuFailure, SkuIsPresentInCarts}
import failures.Failures
import models.objects._
import shapeless._
import utils.JsonFormatters
import utils.aliases._
import utils.db.ExPostgresDriver.api._
import utils.db._
import com.github.tminglei.slickpg._
import models.cord.lineitems.CartLineItems

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

  def mustNotBeArchived[T](target: T, targetId: Any): Failures Xor Sku = {
    if (archivedAt.isEmpty) Xor.right(this)
    else Xor.left(LinkArchivedSkuFailure(target, targetId, code).single)
  }

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
  def filterByContextAndId(contextId: Int, id: Int): QuerySeq =
    filter(_.contextId === contextId).filter(_.formId === id)
  def filterByCode(code: String): QuerySeq =
    filter(_.code.toLowerCase === code.toLowerCase)
  def findOneByCode(code: String): DBIO[Option[Sku]] =
    filter(_.code.toLowerCase === code.toLowerCase).one
}
