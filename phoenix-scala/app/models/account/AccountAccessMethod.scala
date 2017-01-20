package models.account

import java.time.Instant

import cats.data.{Validated, ValidatedNel, Xor}
import cats.implicits._
import failures._
import shapeless._
import slick.driver.PostgresDriver.api._
import utils.HashPasswords
import utils.HashPasswords.HashAlgorithm
import utils.Validation
import utils.aliases._
import utils.db._
import com.typesafe.scalalogging.LazyLogging
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import utils.FoxConfig._

case class AccountAccessMethod(id: Int = 0,
                               accountId: Int,
                               name: String,
                               hashedPassword: String,
                               algorithm: HashAlgorithm,
                               createdAt: Instant = Instant.now,
                               updatedAt: Instant = Instant.now,
                               deletedAt: Option[Instant] = None)
    extends FoxModel[AccountAccessMethod] {

  def updatePassword(newPassword: String): AccountAccessMethod = {
    this.copy(hashedPassword = algorithm.hasher.generateHash(newPassword))
  }

  def checkPassword(password: String): Boolean = {
    algorithm.hasher.checkHash(password, hashedPassword)
  }
}

object AccountAccessMethod extends LazyLogging {

  private val overridenPasswordsHashAlgorithm: Option[HashAlgorithm] =
    config.getOptString("app.overrideHashPasswordAlgorithm").flatMap {
      case "scrypt"    ⇒ Some(HashPasswords.SCrypt)
      case "plainText" ⇒ Some(HashPasswords.PlainText)
      case unknown ⇒
        logger.error(
            s"Unsupported hash password algorithm $unknown. Check app.overrideHashPasswordAlgorithm option")
        None
    }

  val passwordsHashAlgorithm: HashAlgorithm = overridenPasswordsHashAlgorithm match {
    case Some(algo) ⇒
      logger.info(s"Switch to overridden password hash algorithm: $algo")
      algo
    case None ⇒ HashPasswords.SCrypt
  }

  def build(accountId: Int,
            name: String,
            password: String,
            hashAlgorithm: HashAlgorithm = passwordsHashAlgorithm): AccountAccessMethod =
    AccountAccessMethod(accountId = accountId,
                        name = name,
                        algorithm = hashAlgorithm,
                        hashedPassword = hashAlgorithm.hasher.generateHash(password))

  def buildInitial(accountId: Int,
                   name: String = "login",
                   algorithm: HashAlgorithm = passwordsHashAlgorithm): AccountAccessMethod =
    AccountAccessMethod(accountId = accountId,
                        name = name,
                        hashedPassword = "", // should be invalid at initial
                        algorithm = algorithm)
}

class AccountAccessMethods(tag: Tag)
    extends FoxTable[AccountAccessMethod](tag, "account_access_methods") {

  import AccountAccessMethods.PasswordAlgorithmColumn

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

  implicit lazy val PasswordAlgorithmColumn: JdbcType[HashAlgorithm] with BaseTypedType[
      HashAlgorithm] = {
    MappedColumnType.base[HashAlgorithm, Int](c ⇒ c.code, {
      case HashPasswords.SCrypt.code    ⇒ HashPasswords.SCrypt
      case HashPasswords.PlainText.code ⇒ HashPasswords.PlainText
      case j                            ⇒ HashPasswords.UnknownAlgorithm(j)
    })
  }

  val returningLens: Lens[AccountAccessMethod, Int] = lens[AccountAccessMethod].id

  def findByAccountId(accountId: Int): QuerySeq =
    filter(_.accountId === accountId)

  def findOneByAccountIdAndName(accountId: Int, name: String): DBIO[Option[AccountAccessMethod]] =
    filter(_.accountId === accountId).filter(_.name === name).one

  def findOneByIdAndName(id: Int, name: String): DBIO[Option[AccountAccessMethod]] =
    filter(_.id === id).filter(_.name === name).one
}
