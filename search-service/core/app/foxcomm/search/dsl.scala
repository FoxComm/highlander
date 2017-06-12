package foxcomm.search.dsl

import cats.data.NonEmptyList
import io.circe.Decoder.Result
import io.circe.{Decoder, HCursor}
import scala.collection.immutable

final case class FCQuery(query: NonEmptyList[QueryFunction])
object FCQuery {
  implicit val decoder: Decoder[FCQuery] = new Decoder[FCQuery] {
    def apply(c: HCursor): Result[FCQuery] = ???
  }
}

sealed trait QueryField
object QueryField {
  final case class Single(field: String) extends QueryField
  final case class Multiple(fields: NonEmptyList[String]) extends QueryField
  final case class Compound(field: String) extends QueryField
}

sealed trait QueryValue[T]
object QueryValue {
  final case class Single[T](value: T) extends QueryValue[T]
  final case class Multiple[T](values: NonEmptyList[T]) extends QueryValue[T]
  final case class Map[T](map: immutable.Map[RangeFunction, T]) extends QueryValue[T]
}

sealed trait RangeFunction
object RangeFunction {
  case object Lt extends RangeFunction
  case object Lte extends RangeFunction
  case object Gt extends RangeFunction
  case object Gte extends RangeFunction
  case object Eq extends RangeFunction
  case object Neq extends RangeFunction
}

sealed trait QueryFunction {
  type Value

  def value: QueryValue[Value]
}
object QueryFunction {
  sealed trait FilterContext[T] extends QueryFunction {
    type Value = T
  }
  sealed trait QueryContext[T] extends QueryFunction {
    type Value = T
  }
  sealed trait FieldContext {
    def in: QueryField
  }
  final case class Contains[T](in: QueryField, value: QueryValue[T]) extends QueryContext[T] with FieldContext
  final case class Matches[T](in: QueryField, value: QueryValue[T]) extends QueryContext[T] with FieldContext
  final case class Range[T](in: QueryField, value: QueryValue.Map[T]) extends FilterContext[T] with FieldContext
  final case class Is[T](in: QueryField, value: QueryValue[T]) extends FilterContext[T] with FieldContext
  final case class State(value: QueryValue.Single[String]) extends FilterContext[String]
}
