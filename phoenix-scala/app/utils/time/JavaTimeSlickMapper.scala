package utils.time

import java.sql.Timestamp
import java.time.{ZoneOffset, ZoneId}

import slick.driver.PostgresDriver
import slick.driver.PostgresDriver.api._

object JavaTimeSlickMapper {
  import java.time
  private val UTC = ZoneId.of("UTC")

  /**
    * Provides a mapping between java.time.Instant and the Postgres timestamp type.
    *
    * Existing solutions had the issue of mapping timestamps without zone incorrectly to
    * LocalDateTime, which is a concept which members can not be reprensented in the time-line.
    *
    * While a timestamp without zone in Postgres has a implicit zone of UTC,
    * [[java.time.LocalDateTime]] has no zone information - it’s use for instance birthdays
    *
    * Mappers to [[java.time.ZonedDateTime]] don’t feel right as well since most of the time
    * we don’t care about the zone
    */
  implicit def instantAndTimestampWithoutZone: BaseColumnType[time.Instant] =
    PostgresDriver.MappedColumnType.base[time.Instant, Timestamp](
      instant ⇒ Timestamp.valueOf(instant.atZone(UTC).toLocalDateTime),
      timestamp ⇒ timestamp.toLocalDateTime.toInstant(ZoneOffset.UTC))
}
