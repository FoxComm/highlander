package utils

import org.joda.time.DateTime
import slick.driver.PostgresDriver.api._

trait RichTable {
  implicit val JavaUtilDateMapper =
    MappedColumnType .base[java.util.Date, java.sql.Timestamp] (
      d => new java.sql.Timestamp(d.getTime),
      d => new java.util.Date(d.getTime))

  implicit val JodaDataMapper = MappedColumnType.base[DateTime, java.util.Date] (
    dateTime => new java.util.Date(dateTime.getMillis),
    date => new DateTime(date)
  )
}
