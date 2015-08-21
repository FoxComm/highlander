import org.joda.time.DateTime
import slick.dbio.DBIO
import slick.driver.PostgresDriver.api._
import utils.Strings._
import scala.language.implicitConversions
import scala.concurrent.Future

package object utils {
  implicit class RunOnDbIO[R](dbio: DBIO[R]) {
    def run()(implicit db: Database): Future[R] = {
      db.run(dbio)
    }
  }

  def friendlyClassName[A](a: A): String = a.getClass.getSimpleName.replaceAll("""\$""", "").lowerCaseFirstLetter

  implicit def caseClassToMap(cc: Product): Map[String, Any] = {
    val values = cc.productIterator
    cc.getClass.getDeclaredFields.map( _.getName -> values.next ).toMap
  }

  object Joda {
    implicit def dateTimeOrdering: Ordering[DateTime] = Ordering.fromLessThan(_ isBefore _)
  }
}
