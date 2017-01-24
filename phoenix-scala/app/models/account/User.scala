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
                deletedAt: Option[Instant] = None,
                isMigrated: Boolean = false)
    extends FoxModel[User]
    with Validation[User] {

  import Validation._

  def mustHaveCredentials: Failures Xor User = email match {
    case Some(e) ⇒ Xor.Right(this)
    case _       ⇒ Xor.Left(UserMustHaveCredentials.single)
  }

  def mustNotBeBlacklisted: Failures Xor User = {
    if (isBlacklisted) Xor.Left(UserIsBlacklisted(id).single)
    else Xor.Right(this)
  }

  def mustNotBeMigrated: Failures Xor User = {
    if (isMigrated) Xor.Left(UserIsMigrated(id).single)
    else Xor.Right(this)
  }

  override def validate: ValidatedNel[Failure, User] = {
    (nameValid |@| emailValid).map {
      case _ ⇒
        this
    }
  }

  private def nameValid =
    if (name.isEmpty) Validated.Valid(this)
    else
      (notEmpty(name, "name") |@| notEmpty(name.getOrElse(""), "name") |@| matches(
              name.getOrElse(""),
              User.namePattern,
              "name")).map {
        case _ ⇒
          this
      }
  private def emailValid =
    if (email.isEmpty) Validated.Valid(this)
    else
      (notEmpty(email, "email") |@| notEmpty(email.getOrElse(""), "email")).map {
        case _ ⇒
          this
      }
}

object User {
  val namePattern = "[^@]+"
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
  def isMigrated    = column[Boolean]("is_migrated")

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
     deletedAt,
     isMigrated) <> ((User.apply _).tupled, User.unapply)

  def account = foreignKey(Accounts.tableName, accountId, Accounts)(_.id)
}

case class SecurityData(account: Account, user: User, claims: Account.Claims, roles: List[String])

object Users extends FoxTableQuery[User, Users](new Users(_)) with ReturningId[User, Users] {

  val returningLens: Lens[User, Int] = lens[User].id

  def findByEmail(email: String): QuerySeq = {
    filter(_.email === email)
  }

  def activeUserByEmail(email: Option[String]): QuerySeq =
    filter(c ⇒ c.email === email && !c.isBlacklisted && !c.isDisabled)

  def otherUserByEmail(email: String, accountId: Int): QuerySeq = {
    filter(c ⇒ c.email === email && c.accountId =!= accountId && !c.isBlacklisted && !c.isDisabled)
  }

  def findOneByAccountId(accountId: Int): DBIO[Option[User]] =
    filter(_.accountId === accountId).result.headOption

  def findByAccountId(accountId: Int): QuerySeq =
    filter(_.accountId === accountId)

  def findAllByAccountIds(accountIds: Set[Int]): QuerySeq =
    filter(_.accountId.inSet(accountIds))

  def mustFindByAccountId(accountId: Int)(implicit ec: EC): DbResultT[User] =
    filter(_.accountId === accountId).mustFindOneOr(UserWithAccountNotFound(accountId))

  def createEmailMustBeUnique(email: String)(implicit ec: EC): DbResultT[Unit] =
    findByEmail(email).one.mustNotFindOr(UserEmailNotUnique)

  def updateEmailMustBeUnique(maybeEmail: Option[String], accountId: Int)(
      implicit ec: EC): DbResultT[Unit] =
    maybeEmail match {
      case Some(email) ⇒
        otherUserByEmail(email, accountId).one.mustNotFindOr(UserEmailNotUnique)
      case None ⇒ DbResultT.unit
    }

}
