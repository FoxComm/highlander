package utils

import com.lambdaworks.crypto.SCryptUtil

object Passwords {
  def hashPassword(password: String): String = {
    SCryptUtil.scrypt(password, 65536, 8, 1)
  }

  def checkPassword(password: String, hash: String): Boolean = {
    SCryptUtil.check(password, hash)
  }
}
