package utils

import core.failures.GeneralFailure
import phoenix.models.location._
import phoenix.utils.seeds.Factories
import slick.jdbc.PostgresProfile.api._
import testutils._
import core.db._

class TransactionRollbackTest extends IntegrationTestBase {

  "Transaction must be rolled back if DbResultT's value is Either.left" - {

    "for one comprehension" in {
      (for {
        _ ← * <~ Addresses.create(Factories.address)
        _ ← * <~ DbResultT.failure[Unit](GeneralFailure("boom"))
      } yield {}).gimmeTxnFailures

      Addresses.result.gimme must have size 0
    }

    "for multiple comprehensions" in {
      (for {
        _ ← * <~ Addresses.create(Factories.address)
        _ ← * <~ Addresses.create(Factories.address.copy(name = "Jonh Doe"))
        _ ← * <~ DbResultT.failure[Unit](GeneralFailure("boom"))
      } yield {}).gimmeTxnFailures

      Addresses.result.gimme must have size 0
    }

    "for updates" in {
      val address = Addresses.create(Factories.address).gimme

      (for {
        address1 ← * <~ Addresses.update(address, address.copy(name = "John Doe"))
        _        ← * <~ Addresses.update(address1, address1.copy(name = "Mary Jane"))
        _        ← * <~ DbResultT.failure[Unit](GeneralFailure("boom"))
      } yield {}).gimmeTxnFailures

      Addresses.result.headOption.gimme.value must === (address)
    }
  }
}
