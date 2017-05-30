package phoenix.models.account

import java.time.Instant

import com.typesafe.scalalogging.LazyLogging
import core.FoxConfig.config
import core.db._
import core.utils.{HashAlgorithm, HashAlgorithms}
import shapeless._
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import slick.jdbc.PostgresProfile.api._

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
    this.copy(hashedPassword = algorithm.hasher.generateHash(newPassword), updatedAt = Instant.now)
  }

  def checkPassword(password: String): Boolean = {
    algorithm.hasher.checkHash(password, hashedPassword)
  }
}

object AccountAccessMethod extends LazyLogging {
  val passwordsHashAlgorithm: HashAlgorithm = config.app.overrideHashPasswordAlgorithm match {
    case Some(algo) ⇒
      logger.info(s"Switch to overridden password hash algorithm: $algo")
      algo
    case None ⇒ HashAlgorithms.SCrypt
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
      case HashAlgorithms.SCrypt.code    ⇒ HashAlgorithms.SCrypt
      case HashAlgorithms.PlainText.code ⇒ HashAlgorithms.PlainText
      case j                             ⇒ HashAlgorithms.UnknownAlgorithm(j)
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
