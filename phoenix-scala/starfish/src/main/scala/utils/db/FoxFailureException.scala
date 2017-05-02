package utils.db

import failures.Failures

case class FoxFailureException(failures: Failures)
    extends Exception(failures.flatten.mkString("\n"))
