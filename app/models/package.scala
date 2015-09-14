package object models {
  implicit val javaTimeSlickMapper = time.JavaTimeSlickMapper.instantAndTimestampWithoutZone
}
