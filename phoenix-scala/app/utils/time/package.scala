package utils

import java.time.{Duration, Instant, ZoneId, ZonedDateTime}

package object time {
  val UTC: ZoneId = ZoneId.of("UTC")

  def today: ZonedDateTime = Instant.now().atZone(UTC)

  def yesterday: ZonedDateTime = Instant.now().atZone(UTC).minusDays(1)

  implicit class RichInstant(val instant: Instant) extends AnyVal {
    def minuteOfHour: Int = utc.getMinute

    // The follow methods assume UTC
    def plusMinutes(minutes: Long): Instant  = instant.plusSeconds(minutes * 60)
    def minusMinutes(minutes: Long): Instant = plusMinutes(0 - minutes)

    def durationUntil(other: Instant): Duration = Duration.between(instant, other)

    def isBeforeNow: Boolean = instant.toEpochMilli <= Instant.now.toEpochMilli
    def utc: ZonedDateTime   = instant.atZone(UTC)
  }

  implicit class RichDuration(val duration: Duration) extends AnyVal {
    def getMinutes: Int = (duration.getSeconds.toDouble / 60).toInt
  }
}
