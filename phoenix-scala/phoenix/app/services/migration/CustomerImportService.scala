package services.migration

import cats.implicits._
import models.account._
import models.customer._
import payloads.CustomerPayloads.CreateCustomerPayload
import responses.CustomerResponse._
import services.LogActivity
import services.account.{AccountCreateContext, AccountManager}
import utils.aliases._
import utils.db._

object CustomerImportService {

  def create(payload: CreateCustomerPayload,
             admin: Option[User] = None,
             context: AccountCreateContext)(implicit ec: EC, db: DB, ac: AC): DbResultT[Root] =
    for {
      scope ← * <~ Scopes.mustFindById404(context.scopeId)
      user ← * <~ AccountManager.createUser(name = payload.name,
                                            email = payload.email.toLowerCase.some,
                                            password = payload.password,
                                            context = context,
                                            checkEmail = true,
                                            isMigrated = true)
      custData ← * <~ CustomersData.create(
                    CustomerData(accountId = user.accountId,
                                 userId = user.id,
                                 scope = scope.ltree,
                                 isGuest = payload.isGuest.getOrElse(false)))
      result = build(user, custData)
      _ ← * <~ LogActivity().withScope(scope.ltree).customerCreated(result, admin)
    } yield result

}
