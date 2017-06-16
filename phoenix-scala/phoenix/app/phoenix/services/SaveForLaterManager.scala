package phoenix.services

import cats.implicits._
import core.db._
import core.failures.NotFoundFailure404
import objectframework.models.ObjectContext
import phoenix.failures.AlreadySavedForLater
import phoenix.models.account.{User, Users}
import phoenix.models.{SaveForLater, SaveForLaters}
import phoenix.responses.SaveForLaterResponse
import phoenix.services.inventory.SkuManager
import slick.jdbc.PostgresProfile.api._

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
      sku      ← * <~ SkuManager.mustFindSkuByContextAndCode(context.id, skuCode)
      _ ← * <~ SaveForLaters
           .find(accountId = customer.accountId, skuId = sku.id)
           .mustNotFindOneOr(AlreadySavedForLater(accountId = customer.accountId, skuId = sku.id))
      _        ← * <~ SaveForLaters.create(SaveForLater(accountId = customer.accountId, skuId = sku.id))
      response ← * <~ findAllDbio(customer, context.id)
    } yield response

  def deleteSaveForLater(id: Int)(implicit ec: EC, db: DB): DbResultT[Unit] =
    SaveForLaters.deleteById(id, DbResultT.unit, i ⇒ NotFoundFailure404(SaveForLater, i))

  private def findAllDbio(customer: User, contextId: Int)(implicit ec: EC, db: DB): DbResultT[SavedForLater] =
    for {
      sfls ← * <~ SaveForLaters.filter(_.accountId === customer.accountId).result
      r ← * <~ DbResultT.seqFailuresToWarnings(
           sfls.toList.map(sfl ⇒ SaveForLaterResponse.forSkuId(sfl.skuId, contextId)), {
             case _ ⇒ true
           })
    } yield r
}
