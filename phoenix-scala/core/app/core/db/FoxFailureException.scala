package core.db

import core.failures.Failures

case class FoxFailureException(failures: Failures) extends Exception(failures.flatten.mkString("\n"))
