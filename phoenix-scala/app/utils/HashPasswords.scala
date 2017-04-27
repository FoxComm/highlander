package utils

import scala.util.Try
import com.lambdaworks.crypto.SCryptUtil
import com.pellucid.sealerate

trait HashPasswords {
  def generateHash(password: String): String
  def checkHash(password: String, hash: String): Boolean
}

object HashPasswords {

  sealed trait HashAlgorithm {
    val code: Int /** used at db column */
    val hasher: HashPasswords
  }

  case object SCrypt extends HashAlgorithm {
    val code: Int          = 0
    val hasher: SCryptImpl = SCryptImpl(65536, 8, 1)
  }

  case object PlainText extends HashAlgorithm with HashPasswords {
    val code: Int             = 1
    val hasher: HashPasswords = this

    def generateHash(password: String): String =
      password

    def checkHash(password: String, hash: String): Boolean =
      password == hash
  }

  implicit object HashAlgorithm extends ADT[HashAlgorithm] {
    def types = sealerate.collect[HashAlgorithm]
  }

  case class SCryptImpl(cpuCost: Int, memCost: Int, parallelization: Int) extends HashPasswords {
    def generateHash(password: String): String = {
      SCryptUtil.scrypt(password, cpuCost, memCost, parallelization)
    }

    def checkHash(password: String, hash: String): Boolean = {
      Try { SCryptUtil.check(password, hash) }.getOrElse(false)
    }
  }

  case class UnknownAlgorithm(code: Int = 65535) extends HashAlgorithm with HashPasswords {
    val hasher: HashPasswords = this

    def generateHash(password: String): String             = ""
    def checkHash(password: String, hash: String): Boolean = false
  }

}
