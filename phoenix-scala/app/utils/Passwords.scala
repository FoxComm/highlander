package utils

import scala.util.Try
import com.lambdaworks.crypto.SCryptUtil

object Passwords {
  def hashPassword(password: String): String = {
    SCryptUtil.scrypt(password, 65536, 8, 1)
  }

  def checkPassword(password: String, hash: String): Boolean = {
    Try { SCryptUtil.check(password, hash) }.getOrElse(false)
  }
}
