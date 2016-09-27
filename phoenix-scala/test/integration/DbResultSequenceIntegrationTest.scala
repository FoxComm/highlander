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
      val sux: Seq[DbResultT[User]] = Seq(1, 2, 3).map { i ⇒
        Users.create(User(accountId = 100))
      }
      val cool: DbResultT[Seq[User]] = DbResultT.sequence(sux)

      val result = cool.runTxn().futureValue.leftVal

      val allAccounts = Users.gimme
      allAccounts mustBe empty
    }

    "must collect all errors" in {
      Accounts.create(Account()).gimme
      val sux: Seq[DbResultT[User]] = Seq(1, 2, 3).map { i ⇒
        Users.create(User(accountId = 1))
      }
      val cool: DbResultT[Seq[User]] = DbResultT.sequence(sux)

      val failures = cool.run().futureValue.leftVal
      val expectedFailure = DatabaseFailure(
          "ERROR: duplicate key value violates unique constraint \"users_account_idx\"\n" +
            "  Detail: Key (account_id)=(1) already exists.")
      failures must === (expectedFailure.single)

      val allAccounts = Users.gimme
      allAccounts must have size 1
      allAccounts.head.accountId must === (1)
    }
  }
}
