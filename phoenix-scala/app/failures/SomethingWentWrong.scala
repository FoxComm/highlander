package failures

import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import slick.driver.PostgresDriver.api._
import slick.lifted.Query
import utils.aliases.{SF, SL}
import utils.codeSource

case class SomethingWentWrong(desc: String, debugInfo: String) extends Failure {
  override def description: String   = desc
  override def debug: Option[String] = debugInfo.some
}

object SomethingWentWrong extends LazyLogging {
  def apply(details: String)(implicit sl: SL, sf: SF): SomethingWentWrong = {
    val debug = s"$codeSource: unexpected failure!\n    Details: $details"
    SomethingWentWrong("Oops! Something went wrong!", debug)
  }

  def apply[E, U, C[_]](query: Query[E, U, C])(implicit sl: SL, sf: SF): SomethingWentWrong = {
    val sql = query.result.statements.mkString
      .replaceAll("\"", "")
      .replaceAll("from", "\nfrom")
      .replaceAll("where", "\nwhere")
    apply(s"unexpected empty result set from query!\n$sql;")
  }

  def apply(failures: Failures)(implicit sl: SL, sf: SF): SomethingWentWrong =
    SomethingWentWrong(failures.flatten.mkString("\n"))
}
