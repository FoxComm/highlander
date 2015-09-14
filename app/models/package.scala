import utils.time.JavaTimeSlickMapper

package object models {
  implicit val javaTimeSlickMapper = JavaTimeSlickMapper.instantAndTimestampWithoutZone
}
