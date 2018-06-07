package foxcomm.agni.dsl.query

import scala.language.higherKinds
import cats.data.{NonEmptyList, NonEmptyVector}
import cats.implicits._
import foxcomm.agni.dsl._
import io.circe._
import io.circe.generic.extras.semiauto._
import shapeless._

private[query] trait QueryData {
  type QueryValueF[F[_], T] = T :+: F[T] :+: CNil
  implicit def decodeQueryValueF[F[_], T](implicit tD: Decoder[T],
                                          fD: Decoder[F[T]]): Decoder[QueryValueF[F, T]] =
    decodeCoproduct[T, F[T] :+: CNil]

  type QueryValue[T] = QueryValueF[NonEmptyList, T]

  type CompoundValue = QueryValue[JsonNumber] :+: QueryValue[String] :+: CNil
  implicit val decodeCompoundValue: Decoder[CompoundValue] =
    decodeCoproduct[QueryValue[JsonNumber], QueryValue[String] :+: CNil]
      .withErrorMessage("Value must be single or non-empty array of uniformly typed elements")

  type Field = QueryValueF[NonEmptyVector, String]
  implicit val decodeField: Decoder[Field] = Decoder.decodeString.map { s ⇒
    val xs = s.split("\\.")
    if (xs.length > 1) Coproduct[Field](NonEmptyVector.of(xs.head, xs.tail: _*))
    else Coproduct[Field](s)
  }

  type RangeValue = RangeBound[JsonNumber] :+: RangeBound[String] :+: CNil
  implicit val decodeRangeValue: Decoder[RangeValue] =
    decodeCoproduct[RangeBound[JsonNumber], RangeBound[String] :+: CNil]
      .withErrorMessage(
        "Error in range function bounds. " +
          "Please make sure that you supplied valid operators and that you have uniformly typed values.")

  sealed trait RangeFunction
  object RangeFunction {
    sealed trait LowerBound extends RangeFunction {
      def withBound: Boolean
    }
    case object Gt extends RangeFunction with LowerBound {
      def withBound: Boolean = false
    }
    case object Gte extends RangeFunction with LowerBound {
      def withBound: Boolean = true
    }

    sealed trait UpperBound extends RangeFunction {
      def withBound: Boolean
    }
    case object Lt extends RangeFunction with UpperBound {
      def withBound: Boolean = false
    }
    case object Lte extends RangeFunction with UpperBound {
      def withBound: Boolean = true
    }

    implicit val decodeRangeFunction: KeyDecoder[RangeFunction] = KeyDecoder.instance {
      case "lt" | "<"   ⇒ Some(Lt)
      case "lte" | "<=" ⇒ Some(Lte)
      case "gt" | ">"   ⇒ Some(Gt)
      case "gte" | ">=" ⇒ Some(Gte)
      case _            ⇒ None
    }
  }

  sealed case class RangeBound[T](lower: Option[(RangeFunction.LowerBound, T)],
                                  upper: Option[(RangeFunction.UpperBound, T)])
  object RangeBound {
    implicit def decodeRangeBound[T: Decoder]: Decoder[RangeBound[T]] =
      Decoder.decodeMapLike[Map, RangeFunction, T].emap { map ⇒
        val lbs = map.view.collect {
          case (lb: RangeFunction.LowerBound, v) ⇒ lb → v
        }.toList
        val ubs = map.view.collect {
          case (ub: RangeFunction.UpperBound, v) ⇒ ub → v
        }.toList

        if (lbs.size > 1) Either.left("Only single lower bound can be specified")
        else if (ubs.size > 1) Either.left("Only single upper bound can be specified")
        else Either.right(RangeBound(lbs.headOption, ubs.headOption))
      }
  }

  sealed trait QueryField {
    def toNEL: NonEmptyList[Field]

    def toList: List[Field] = toNEL.toList
  }
  object QueryField {
    sealed case class Single(field: Field) extends QueryField {
      def toNEL: NonEmptyList[Field] = NonEmptyList.of(field)
    }
    object Single {
      implicit val decodeSingle: Decoder[Single] = Decoder[Field]
        .map(Single(_))
        .withErrorMessage("Query field must be a single string")
    }

    sealed case class Multiple(fields: NonEmptyList[Field]) extends QueryField {
      def toNEL: NonEmptyList[Field] = fields
    }
    object Multiple {
      implicit val decodeMultiple: Decoder[Multiple] =
        Decoder
          .decodeNonEmptyList[Field]
          .map(Multiple(_))
          .withErrorMessage("Query field must be a non-empty array of strings")
    }

    implicit val decodeQueryField: Decoder[QueryField] =
      (Decoder[Single].map(identity[QueryField]) or Decoder[Multiple].map(identity[QueryField]))
        .withErrorMessage("Query field must be either a single string or a non-empty array of strings")
  }

  sealed trait QueryContext
  object QueryContext {
    case object filter extends QueryContext
    case object must   extends QueryContext
    case object should extends QueryContext
    case object not    extends QueryContext

    implicit val decodeQueryContext: Decoder[QueryContext] = deriveEnumerationDecoder[QueryContext]
      .withErrorMessage("Unknown query function context")
  }
}
