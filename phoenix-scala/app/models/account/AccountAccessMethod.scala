package models.account

import java.time.Instant

import cats.data.{Validated, ValidatedNel, Xor}
import cats.implicits._
import failures._
import shapeless._
import slick.driver.PostgresDriver.api._
import utils.Passwords.{hashPassword, checkPassword ⇒ scryptCheckPassword}
import utils.Validation
import utils.aliases._
import utils.db._
import AccountAccessMethod._
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType

case class AccountAccessMethod(id: Int = 0,
                               accountId: Int,
                               name: String,
                               hashedPassword: String,
                               algorithm: HashAlgorithm = Scrypt,
                               createdAt: Instant = Instant.now,
                               updatedAt: Instant = Instant.now,
                               deletedAt: Option[Instant] = None)
    extends FoxModel[AccountAccessMethod] {

  def updatePassword(newPassword: String): AccountAccessMethod = {
    this.copy(hashedPassword = hashPassword(newPassword))
  }

  def checkPassword(password: String): Boolean = {
    algorithm match {
      case Scrypt    ⇒ scryptCheckPassword(password, hashedPassword)
      case PlainText ⇒ password == hashedPassword //TODO remove , only fo demo.
      case _         ⇒ false
    }
  }
}

object AccountAccessMethod {

  sealed trait HashAlgorithm {
    val code: Int
  }
  case object Scrypt extends HashAlgorithm {
    val code: Int = 0
  }
  case object PlainText extends HashAlgorithm {
    val code: Int = 1
  }

  case class UnknownAlgorithm(code: Int) extends HashAlgorithm

  implicit val PasswordAlgorithmColumn: JdbcType[HashAlgorithm] with BaseTypedType[HashAlgorithm] = {
    MappedColumnType.base[HashAlgorithm, Int](c ⇒ c.code, {
      case 0 ⇒ Scrypt
      case 1 ⇒ PlainText
      case j ⇒ UnknownAlgorithm(j)
    })
  }

  def build(accountId: Int, name: String, password: String): AccountAccessMethod =
    AccountAccessMethod(accountId = accountId,
                        name = name,
                        hashedPassword = hashPassword(password))

  def buildInitial(accountId: Int,
                   name: String = "login",
                   algorithm: HashAlgorithm = Scrypt): AccountAccessMethod =
    AccountAccessMethod(accountId = accountId,
                        name = name,
                        hashedPassword = "",
                        algorithm = algorithm)
}

class AccountAccessMethods(tag: Tag)
    extends FoxTable[AccountAccessMethod](tag, "account_access_methods") {
  def id             = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def accountId      = column[Int]("account_id")
  def name           = column[String]("name")
  def hashedPassword = column[String]("hashed_password")
  def algorithm      = column[HashAlgorithm]("algorithm")
  def createdAt      = column[Instant]("created_at")
  def updatedAt      = column[Instant]("updated_at")
  def deletedAt      = column[Option[Instant]]("deleted_at")

  def * =
    (id, accountId, name, hashedPassword, algorithm, createdAt, updatedAt, deletedAt) <> ((AccountAccessMethod.apply _).tupled, AccountAccessMethod.unapply)
}

object AccountAccessMethods
    extends FoxTableQuery[AccountAccessMethod, AccountAccessMethods](new AccountAccessMethods(_))
    with ReturningId[AccountAccessMethod, AccountAccessMethods]
    with SearchById[AccountAccessMethod, AccountAccessMethods] {

  val returningLens: Lens[AccountAccessMethod, Int] = lens[AccountAccessMethod].id

  def findByAccountId(accountId: Int): QuerySeq =
    filter(_.accountId === accountId)

  def findOneByAccountIdAndName(accountId: Int, name: String): DBIO[Option[AccountAccessMethod]] =
    filter(_.accountId === accountId).filter(_.name === name).one

  def findOneByIdAndName(id: Int, name: String): DBIO[Option[AccountAccessMethod]] =
    filter(_.id === id).filter(_.name === name).one
}
