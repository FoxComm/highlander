import utils.Money
import utils.time.JavaTimeSlickMapper

package object models {
  implicit val javaTimeSlickMapper      = JavaTimeSlickMapper.instantAndTimestampWithoutZone
  implicit val currencyColumnTypeMapper = Money.currencyColumnType
}
