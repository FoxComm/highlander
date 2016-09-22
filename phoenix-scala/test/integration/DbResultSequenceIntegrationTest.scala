import cats.implicits._
import failures.DatabaseFailure
import models.account._
import util.IntegrationTestBase
import utils.db._
import utils.seeds.Seeds.Factories

class DbResultSequenceIntegrationTest extends IntegrationTestBase {

  "DbResultT#sequence" - {
    "must convert Seq[DbResultT[A]] into DbResultT[Seq[A]]" in {
      val sux: Seq[DbResultT[Account]] = Seq(1, 2, 3).map { i ⇒
        Accounts.create(Account(ratchet = i))
      }
      val cool: DbResultT[Seq[Account]] = DbResultT.sequence(sux)
      cool.gimme

      val allAccounts = Accounts.gimme
      allAccounts must have size 3
      allAccounts.map(_.ratchet) must contain allOf (1, 2, 3)
    }

    "must rollback transaction on errors" in {
      val sux: Seq[DbResultT[Account]] = Seq(1, 2, 3).map { i ⇒
        Accounts.create(Account())
      }
      val cool: DbResultT[Seq[Account]] = DbResultT.sequence(sux)

      val result = cool.runTxn().futureValue.leftVal

      val allAccounts = Accounts.gimme
      allAccounts mustBe empty
    }

    "must collect all errors" in {
      val sux: Seq[DbResultT[Account]] = Seq(1, 2, 3).map { i ⇒
        Accounts.create(Account(ratchet = 1))
      }
      val cool: DbResultT[Seq[Account]] = DbResultT.sequence(sux)

      val failures = cool.run().futureValue.leftVal
      val expectedFailure = DatabaseFailure(
          "ERROR: duplicate key value violates unique constraint \"customers_active_non_guest_email\"\n" +
            "  Detail: Key (email, is_disabled, is_guest)=(boom, f, f) already exists.")
      failures must === (expectedFailure.single)

      val allAccounts = Accounts.gimme
      allAccounts must have size 1
      allAccounts.head.ratchet must === (1)
    }
  }
}
