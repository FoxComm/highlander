package phoenix.models.account

import java.time.Instant

import cats.data.{Validated, ValidatedNel}
import cats.implicits._
import core.db._
import core.failures._
import core.utils.Validation
import phoenix.failures.UserFailures._
import phoenix.models.customer.CustomersData
import shapeless._
import slick.jdbc.PostgresProfile.api._

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

  def mustHaveCredentials: Either[Failures, User] = email match {
    case Some(e) ⇒ Either.right(this)
    case _       ⇒ Either.left(UserMustHaveCredentials.single)
  }

  def mustNotBeBlacklisted: Either[Failures, User] =
    if (isBlacklisted) Either.left(UserIsBlacklisted(accountId).single)
    else Either.right(this)

  def mustNotBeMigrated: Either[Failures, User] =
    if (isMigrated) Either.left(UserIsMigrated(id).single)
    else Either.right(this)

  override def validate: ValidatedNel[Failure, User] =
    (nameValid |@| emailValid).map {
      case _ ⇒
        this
    }

  private def nameValid =
    if (name.isEmpty) Validated.Valid(this)
    else
      (notEmpty(name, "name") |@| notEmpty(name.getOrElse(""), "name") |@| matches(name.getOrElse(""),
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

  def findByEmail(email: String): QuerySeq =
    filter(_.email === email)

  def findNonGuestByEmail(email: String): QuerySeq =
    findByEmail(email)
      .joinLeft(CustomersData)
      .on(_.accountId === _.accountId)
      .filterNot { case (_, data) ⇒ data.map(_.isGuest).getOrElse(false) }
      .map { case (user, _) ⇒ user }

  def activeUserByEmail(email: Option[String]): QuerySeq =
    filter(c ⇒ c.email === email && !c.isBlacklisted && !c.isDisabled)

  def otherUserByEmail(email: String, accountId: Int): QuerySeq =
    filter(c ⇒ c.email === email && c.accountId =!= accountId && !c.isBlacklisted && !c.isDisabled)

  def findOneByAccountId(accountId: Int): DBIO[Option[User]] =
    filter(_.accountId === accountId).result.headOption

  def mustFindByAccountId(accountId: Int)(implicit ec: EC): DbResultT[User] =
    filter(_.accountId === accountId).mustFindOneOr(UserWithAccountNotFound(accountId))

  def createEmailMustBeUnique(email: String)(implicit ec: EC): DbResultT[Unit] =
    findNonGuestByEmail(email).one.mustNotFindOr(UserEmailNotUnique)

  def updateEmailMustBeUnique(maybeEmail: Option[String], accountId: Int)(implicit ec: EC): DbResultT[Unit] =
    maybeEmail match {
      case Some(email) ⇒
        otherUserByEmail(email, accountId).one.mustNotFindOr(UserEmailNotUnique)
      case None ⇒ ().pure[DbResultT]
    }

}
