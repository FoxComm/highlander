package foxcomm.agni.dsl

import cats.data.NonEmptyList

package object query extends QueryData with QueryFunctions {
  implicit class RichQueryValue[T](val qv: QueryValue[T]) extends AnyVal {
    def toNEL: NonEmptyList[T] = qv.eliminate(NonEmptyList.of(_), _.eliminate(identity, _.impossible))

    def toList: List[T] = toNEL.toList
  }

  implicit class RichCompoundValue(val cv: CompoundValue) extends AnyVal {
    def toNEL: NonEmptyList[AnyRef] = cv.eliminate(_.toNEL, _.eliminate(_.toNEL, _.impossible))

    def toList: List[AnyRef] = toNEL.toList
  }
}
