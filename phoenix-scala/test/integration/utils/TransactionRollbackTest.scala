package utils

import failures.GeneralFailure
import models.location._
import slick.driver.PostgresDriver.api._
import testutils._
import utils.db._
import utils.seeds.Seeds.Factories

class TransactionRollbackTest extends IntegrationTestBase {

  "Transaction must be rolled back if DbResultT's value is Xor.left" - {

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
