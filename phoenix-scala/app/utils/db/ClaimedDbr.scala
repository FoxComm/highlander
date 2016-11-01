package utils.db

import models.account.Account
import utils.db._

case class ClaimedDbr[A](dbr: DbResultT[A], claims: Account.ClaimSet)
