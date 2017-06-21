import cats.data._
import cats.implicits._
import core.failures._
import phoenix.models.account._
import testutils._
import core.db._

class DbResultSequenceIntegrationTest extends IntegrationTestBase {

  "DbResultT#sequenceJoiningFailures" - {
    "must convert List[DbResultT[A]] into DbResultT[List[A]]" in {
      val sux: List[DbResultT[Account]] = List(1, 2, 3).map { i ⇒
        Accounts.create(Account(ratchet = i))
      }
      val cool: DbResultT[List[Account]] = DbResultT.seqCollectFailures(sux)
      cool.gimme

      val allAccounts = Accounts.gimme
      allAccounts must have size 3
      allAccounts.map(_.ratchet) must contain allOf (1, 2, 3)
    }

    "must rollback transaction on errors" in {
      val sux: Vector[DbResultT[User]] = (1 to 3).toVector.map { i ⇒
        Users.create(User(accountId = 100))
      }
      val cool: DbResultT[Vector[User]] = DbResultT.seqCollectFailures(sux)

      cool.runTxn().runEmptyA.value.futureValue mustBe 'left

      val allAccounts = Users.gimme
      allAccounts mustBe empty
    }

    "must collect all errors" in {
      val account  = Accounts.create(Account()).gimme
      val numTries = 5
      val sux: List[DbResultT[User]] = (1 to numTries).toList.map { i ⇒
        Users.create(User(accountId = account.id))
      }
      val cool: DbResultT[List[User]] = DbResultT.seqCollectFailures(sux)

      val failures        = cool.gimmeFailures
      val expectedFailure = DatabaseFailure(s"Key (account_id)=(${account.id}) already exists.")

      failures.size must === (numTries - 1)
      failures
        .map(_.description.contains(expectedFailure.description))
        .forall(_ == true) must === (true)

      Users.gimme.onlyElement.accountId must === (account.id)
    }
  }
}
