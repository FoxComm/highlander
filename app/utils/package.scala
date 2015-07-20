import slick.dbio.DBIO

import slick.driver.PostgresDriver.api._
import utils.Strings._

import scala.concurrent.Future

package object utils {
  implicit class RunOnDbIO[R](dbio: DBIO[R]) {
    def run()(implicit db: Database): Future[R] = {
      db.run(dbio)
    }
  }

  def friendlyClassName[A](a: A): String = a.getClass.getSimpleName.replaceAll("""\$""", "").lowerCaseFirstLetter
}
