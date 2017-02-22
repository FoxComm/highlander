package failures

import com.typesafe.scalalogging.LazyLogging
import slick.driver.PostgresDriver.api._
import slick.lifted.Query
import utils.aliases.{SF, SL}
import utils.codeSource

object SomethingWentWrong extends LazyLogging {
  def apply(details: String)(implicit sl: SL, sf: SF): GeneralFailure = {
    logger.error(s"$codeSource: unexpected failure!\n    Details: $details")
    GeneralFailure("Oops! Something went wrong!")
  }

  def apply[E, U, C[_]](query: Query[E, U, C])(implicit sl: SL, sf: SF): GeneralFailure = {
    val sql = query.result.statements.mkString.replaceAll("\"", "")
    apply(s"unexpected empty result set from query\n    $sql;")
  }
}
