package core.utils

import com.lambdaworks.crypto.SCryptUtil
import com.pellucid.sealerate
import core.ADT

import scala.util.Try

trait PasswordHasher {
  def generateHash(password: String): String
  def checkHash(password: String, hash: String): Boolean
}

sealed trait HashAlgorithm {
  val code: Int /** used at db column */
  val hasher: PasswordHasher
}

object HashAlgorithms {

  case object SCrypt extends HashAlgorithm {
    val code: Int          = 0
    val hasher: SCryptImpl = SCryptImpl(65536, 8, 1)
  }

  case object PlainText extends HashAlgorithm with PasswordHasher {
    val code: Int              = 1
    val hasher: PasswordHasher = this

    def generateHash(password: String): String =
      password

    def checkHash(password: String, hash: String): Boolean =
      password == hash
  }

  case class SCryptImpl(cpuCost: Int, memCost: Int, parallelization: Int) extends PasswordHasher {
    def generateHash(password: String): String = {
      SCryptUtil.scrypt(password, cpuCost, memCost, parallelization)
    }

    def checkHash(password: String, hash: String): Boolean = {
      Try { SCryptUtil.check(password, hash) }.getOrElse(false)
    }
  }

  case class UnknownAlgorithm(code: Int = 65535) extends HashAlgorithm with PasswordHasher {
    val hasher: PasswordHasher = this

    def generateHash(password: String): String             = ""
    def checkHash(password: String, hash: String): Boolean = false
  }

  implicit object HashAlgorithm extends ADT[HashAlgorithm] {
    def types = sealerate.collect[HashAlgorithm]
  }

}
