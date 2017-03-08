package services

import cats.implicits._
import failures.{AlreadySavedForLater, NotFoundFailure404}
import models.account.{User, Users}
import models.objects.ObjectContext
import models.{SaveForLater, SaveForLaters}
import responses.SaveForLaterResponse
import services.inventory.ProductVariantManager
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

object SaveForLaterManager {

  type SavedForLater = Seq[SaveForLaterResponse.Root]

  def findAll(accountId: Int, contextId: Int)(implicit db: DB, ec: EC): DbResultT[SavedForLater] =
    for {
      customer ← * <~ Users.mustFindByAccountId(accountId)
      response ← * <~ findAllDbio(customer, contextId)
    } yield response

  def saveForLater(accountId: Int, skuCode: String, context: ObjectContext)(
      implicit db: DB,
      ec: EC): DbResultT[SavedForLater] =
    for {
      customer ← * <~ Users.mustFindByAccountId(accountId)
      variant  ← * <~ ProductVariantManager.mustFindByContextAndCode(context.id, skuCode)
      _ ← * <~ SaveForLaters
           .find(accountId = customer.accountId, productVariantId = variant.id)
           .mustNotFindOneOr(
               AlreadySavedForLater(accountId = customer.accountId, variantId = variant.id))
      _ ← * <~ SaveForLaters.create(
             SaveForLater(accountId = customer.accountId, productVariantId = variant.id))
      response ← * <~ findAllDbio(customer, context.id)
    } yield response

  def deleteSaveForLater(id: Int)(implicit ec: EC, db: DB): DbResultT[Unit] =
    SaveForLaters.deleteById(id, DbResultT.unit, i ⇒ NotFoundFailure404(SaveForLater, i))

  private def findAllDbio(customer: User, contextId: Int)(implicit ec: EC,
                                                          db: DB): DbResultT[SavedForLater] =
    for {
      sfls ← * <~ SaveForLaters.filter(_.accountId === customer.accountId).result
      r ← * <~ DbResultT.seqFailuresToWarnings(
             sfls.toList.map(sfl ⇒
                   SaveForLaterResponse.forVariantId(sfl.productVariantId, contextId)), {
               case _ ⇒ true
             })
    } yield r
}
