package phoenix.services.migration

import cats.implicits._
import core.db._
import phoenix.models.account._
import phoenix.models.customer._
import phoenix.payloads.CustomerPayloads.CreateCustomerPayload
import phoenix.responses.users.CustomerResponse
import phoenix.services.LogActivity
import phoenix.services.account.{AccountCreateContext, AccountManager}
import phoenix.utils.aliases._

object CustomerImportService {

  def create(payload: CreateCustomerPayload,
             admin: Option[User] = None,
             context: AccountCreateContext)(implicit ec: EC, db: DB, ac: AC): DbResultT[CustomerResponse] =
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
      result = CustomerResponse.build(user, custData)
      _ ← * <~ LogActivity().withScope(scope.ltree).customerCreated(result, admin)
    } yield result

}
