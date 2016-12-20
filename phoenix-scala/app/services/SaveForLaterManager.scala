package services

import cats.data.Xor
import failures.{AlreadySavedForLater, Failures, NotFoundFailure404}
import models.account.{User, Users}
import models.objects.ObjectContext
import models.{SaveForLater, SaveForLaters}
import responses.{SaveForLaterResponse, TheResponse}
import services.inventory.ProductVariantManager
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

object SaveForLaterManager {

  type SavedForLater = TheResponse[Seq[SaveForLaterResponse.Root]]

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
           .find(accountId = customer.accountId, variantId = variant.id)
           .mustNotFindOneOr(
               AlreadySavedForLater(accountId = customer.accountId, variantId = variant.id))
      _ ← * <~ SaveForLaters.create(
             SaveForLater(accountId = customer.accountId, variantId = variant.id))
      response ← * <~ findAllDbio(customer, context.id)
    } yield response

  def deleteSaveForLater(id: Int)(implicit ec: EC, db: DB): DbResultT[Unit] =
    SaveForLaters.deleteById(id, DbResultT.unit, i ⇒ NotFoundFailure404(SaveForLater, i))

  private def findAllDbio(customer: User, contextId: Int)(implicit ec: EC,
                                                          db: DB): DBIO[SavedForLater] =
    for {
      sfls ← SaveForLaters.filter(_.accountId === customer.accountId).result
      xors ← DBIO.sequence(
                sfls.map(sfl ⇒ SaveForLaterResponse.forSkuId(sfl.variantId, contextId).value))

      fails = xors.collect { case Xor.Left(f)  ⇒ f }.flatMap(_.toList)
      roots = xors.collect { case Xor.Right(r) ⇒ r }
    } yield TheResponse.build(roots, warnings = Failures(fails: _*))
}
