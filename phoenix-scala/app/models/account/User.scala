package models.account

import java.time.Instant

import cats.data.{Validated, ValidatedNel, Xor}
import cats.implicits._
import failures.UserFailures._
import failures._
import shapeless._
import slick.driver.PostgresDriver.api._
import utils.Validation
import utils.aliases._
import utils.db._

case class User(id: Int = 0,
                accountId: Int,
                name: Option[String] = None,
                email: Option[String] = None,
                phoneNumber: Option[String] = None,
                isDisabled: Boolean = false,
                disabledBy: Option[Int] = None,
                isBlacklisted: Boolean = false,
                blacklistedBy: Option[Int] = None,
                createdAt: Instant = Instant.now,
                updatedAt: Instant = Instant.now,
                deletedAt: Option[Instant] = None)
    extends FoxModel[User]
    with Validation[User] {

  import Validation._

  def mustHaveCredentials: Failures Xor User = (name, email) match {
    case (Some(n), Some(e)) ⇒ Xor.Right(this)
    case _                  ⇒ Xor.Left(UserMustHaveCredentials.single)
  }

  def mustNotBeBlacklisted: Failures Xor User = {
    if (isBlacklisted) Xor.Left(UserIsBlacklisted(id).single)
    else Xor.Right(this)
  }
}

class Users(tag: Tag) extends FoxTable[User](tag, "users") {
  def id            = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def accountId     = column[Int]("account_id")
  def name          = column[Option[String]]("name")
  def email         = column[Option[String]]("email")
  def phoneNumber   = column[Option[String]]("phone_number")
  def isDisabled    = column[Boolean]("is_disabled")
  def disabledBy    = column[Option[Int]]("disabled_by")
  def isBlacklisted = column[Boolean]("is_blacklisted")
  def blacklistedBy = column[Option[Int]]("blacklisted_by")
  def createdAt     = column[Instant]("created_at")
  def updatedAt     = column[Instant]("updated_at")
  def deletedAt     = column[Option[Instant]]("deleted_at")

  def * =
    (id,
     accountId,
     name,
     email,
     phoneNumber,
     isDisabled,
     disabledBy,
     isBlacklisted,
     blacklistedBy,
     createdAt,
     updatedAt,
     deletedAt) <> ((User.apply _).tupled, User.unapply)

  def account    = foreignkey(Accounts.tablename, accountId, Account)(_.id)
}

case class SecurityData(
  account: Account,
  user: User,
  claims: Account.Claims,
  roles: List[String])


object Users extends FoxTableQuery[User, Users](new Users(_)) with ReturningId[User, Users] {

  val returningLens: Lens[User, Int] = lens[User].id

  def findByEmail(email: String): QuerySeq = {
    filter(_.email === email)
  }

  def findOneByAccountId(accountId: Int): DBIO[Option[User]] = 
    filter(_.accountId === accountId).result.headOption

  def findByAccountId(accountId: Int): QuerySeq = 
    filter(_.accountId === accountId)

  def mustFindByAccountId(accountId: Int): DbResultT[User] = 
    filter(_.accountId === accountId).one.mustFindOneOr(UserWithAccountNotFound(accountId))

  def createEmailMustBeUnique(email: String)(implicit ec: EC): DbResultT[Unit] =
    findByEmail(email).one.mustNotFindOr(UserEmailNotUnique)

  def updateEmailMustBeUnique(email: String, userId: Int)(implicit ec: EC): DbResultT[Unit] =
    findByEmail(email).filter(_.id === userId).one.mustNotFindOr(UserEmailNotUnique)

  def getSecurityData(userId: Int) =
    for {
      user          ← Users if user.id === userId
      account       ← user.account
      accountRole   ← AccountRoles if accountRole.accountId === account.id
      permissions   ← 
      link          ← ProductSkuLinks if link.rightId === sku.id
      product       ← Products if product.id === link.rightId
      productShadow ← ObjectShadows if productShadow.id === product.shadowId
    } yield (sku, skuForm, skuShadow, productShadow, lineItems)
}
