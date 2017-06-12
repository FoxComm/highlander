package phoenix.services

import core.db._
import phoenix.failures.CartFailures.CustomerHasNoCart
import phoenix.models.account.User
import phoenix.models.cord._

package object carts {

  def getCartByOriginator(originator: User, refNum: Option[String] = None)(implicit ec: EC,
                                                                           db: DB): DbResultT[Cart] =
    (originator, refNum) match {
      case (_, Some(ref)) ⇒
        Carts.mustFindByRefNum(ref)

      case (originator, _) ⇒
        Carts
          .findByAccountId(originator.accountId)
          .mustFindOneOr(CustomerHasNoCart(originator.accountId))

    }
  //MAXDO: Use claims here to figure out which path to take.
  /*
      case (_, _) ⇒
        DbResultT.failure(EmptyRefNumFailure)
 */
}
