package foxcomm.agni.dsl

import scala.language.higherKinds
import cats.data.{NonEmptyList, NonEmptyVector}
import cats.implicits._
import io.circe._
import io.circe.generic.extras.semiauto._
import scala.util.Try
import shapeless._

object query {
  type QueryValueF[F[_], T] = T :+: F[T] :+: CNil
  type QueryValue[T]        = QueryValueF[NonEmptyList, T]
  type CompoundValue        = QueryValue[JsonNumber] :+: QueryValue[String] :+: CNil
  type Field                = QueryValueF[NonEmptyVector, String]
  type RangeValue           = RangeBound[JsonNumber] :+: RangeBound[String] :+: CNil

  implicit class RichQueryValue[T](val qv: QueryValue[T]) extends AnyVal {
    def toNEL: NonEmptyList[T] = qv.eliminate(NonEmptyList.of(_), _.eliminate(identity, _.impossible))

    def toList: List[T] = toNEL.toList
  }

  implicit class RichCompoundValue(val cv: CompoundValue) extends AnyVal {
    def toNEL: NonEmptyList[AnyRef] = cv.eliminate(_.toNEL, _.eliminate(_.toNEL, _.impossible))

    def toList: List[AnyRef] = toNEL.toList
  }

  implicit def decodeQueryValueF[F[_], T](implicit fD: Decoder[F[T]],
                                          tD: Decoder[T]): Decoder[QueryValueF[F, T]] =
    tD.map(Coproduct[QueryValueF[F, T]](_)) or fD.map(Coproduct[QueryValueF[F, T]](_))

  implicit val decodeCompoundValue: Decoder[CompoundValue] =
    Decoder[QueryValue[JsonNumber]].map(Coproduct[CompoundValue](_)) or
      Decoder[QueryValue[String]].map(Coproduct[CompoundValue](_))

  implicit val decodeField: Decoder[Field] = Decoder.decodeString.map { s ⇒
    val xs = s.split("\\.")
    if (xs.length > 1) Coproduct[Field](NonEmptyVector.of(xs.head, xs.tail: _*))
    else Coproduct[Field](s)
  }

  implicit val decodeRange: Decoder[RangeValue] =
    Decoder[RangeBound[JsonNumber]].map(Coproduct[RangeValue](_)) or
      Decoder[RangeBound[String]].map(Coproduct[RangeValue](_))

  object Boostable {
    private[this] val boostableRegex = "^(\\w+)\\^([0-9]*\\.?[0-9]+)$".r

    def unapply(s: String): Option[(String, Float)] = s match {
      case boostableRegex(f, b) ⇒ Try(f → b.toFloat).toOption
      case _                    ⇒ None
    }

    def default: Float = 1.0f
  }

  sealed trait QueryField {
    def toNEL: NonEmptyList[Field]

    def toList: List[Field] = toNEL.toList
  }
  object QueryField {
    final case class Value(field: String, boost: Option[Float])

    final case class Single(field: Field) extends QueryField {
      def toNEL: NonEmptyList[Field] = NonEmptyList.of(field)
    }
    object Single {
      implicit val decodeSingle: Decoder[Single] = Decoder[Field].map(Single(_))
    }

    final case class Multiple(fields: NonEmptyList[Field]) extends QueryField {
      def toNEL: NonEmptyList[Field] = fields
    }
    object Multiple {
      implicit val decodeMultiple: Decoder[Multiple] =
        Decoder.decodeNonEmptyList[Field].map(Multiple(_))
    }

    implicit val decodeQueryField: Decoder[QueryField] =
      Decoder[Single].map(identity) or Decoder[Multiple].map(identity)
  }

  sealed trait QueryContext
  object QueryContext {
    case object filter extends QueryContext
    case object must   extends QueryContext
    case object should extends QueryContext
    case object not    extends QueryContext

    implicit val decodeQueryContext: Decoder[QueryContext] = deriveEnumerationDecoder[QueryContext]
  }

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
    }
  }

  final case class RangeBound[T](lower: Option[(RangeFunction.LowerBound, T)],
                                 upper: Option[(RangeFunction.UpperBound, T)])
  object RangeBound {
    import RangeFunction._

    implicit def decodeRangeBound[T: Decoder]: Decoder[RangeBound[T]] =
      Decoder.decodeMapLike[Map, RangeFunction, T].emap { map ⇒
        val lbs = map.view.collect {
          case (lb: LowerBound, v) ⇒ lb → v
        }.toList
        val ubs = map.view.collect {
          case (ub: UpperBound, v) ⇒ ub → v
        }.toList

        if (lbs.size > 1) Either.left("Only single lower bound can be specified")
        else if (ubs.size > 1) Either.left("Only single upper bound can be specified")
        else Either.right(RangeBound(lbs.headOption, ubs.headOption))
      }
  }

  sealed trait QueryFunction
  object QueryFunction {
    val Discriminator = "type"

    private def buildQueryFunctionDecoder[A <: QueryFunction](expectedTpe: String, decoder: Decoder[A])(
        onBoost: (Float, HCursor, Decoder[A]) ⇒ Decoder.Result[A]) =
      Decoder.instance { c ⇒
        val tpe = c.downField(Discriminator).focus.flatMap(_.asString)
        tpe match {
          case Some(Boostable(`expectedTpe`, b)) ⇒ onBoost(b, c, decoder)
          case Some(`expectedTpe`)               ⇒ decoder(c)
          case _                                 ⇒ Either.left(DecodingFailure("Unknown query function type", c.history))
        }
      }

    private def buildBoostableDecoder[A <: QueryFunction](expectedTpe: String)(decoder: Decoder[A]) =
      buildQueryFunctionDecoder[A](expectedTpe, decoder)((boost, cursor, decoder) ⇒
        decoder.tryDecode(cursor.withFocus(_.mapObject(_.add("boost", Json.fromFloatOrNull(boost))))))

    private def buildDecoder[A <: QueryFunction](expectedTpe: String)(decoder: Decoder[A]): Decoder[A] =
      buildQueryFunctionDecoder[A](expectedTpe, decoder)((_, cursor, _) ⇒
        Either.left(DecodingFailure(s"$expectedTpe query function is not boostable", cursor.history)))

    sealed trait WithField { this: QueryFunction ⇒
      def field: QueryField
      def boost: Option[Float]
    }
    sealed trait WithContext { this: QueryFunction ⇒
      def ctx: QueryContext
    }

    sealed trait TermLevel extends WithContext { this: QueryFunction ⇒
      def context: Option[QueryContext]

      final def ctx: QueryContext = context.getOrElse(QueryContext.filter)
    }
    sealed trait FullText extends WithContext with WithField { this: QueryFunction ⇒
      def context: Option[QueryContext]
      def in: Option[QueryField]

      final def ctx: QueryContext = context.getOrElse(QueryContext.must)
      final def field: QueryField = in.getOrElse(QueryField.Single(Coproduct("_all")))
    }

    final case class matches private (in: Option[QueryField],
                                      value: QueryValue[String],
                                      context: Option[QueryContext],
                                      boost: Option[Float])
        extends QueryFunction
        with FullText
    object matches {
      implicit val decodeMatches: Decoder[matches] = buildBoostableDecoder("matches")(deriveDecoder[matches])
    }

    final case class equals private (in: QueryField,
                                     value: CompoundValue,
                                     context: Option[QueryContext],
                                     boost: Option[Float])
        extends QueryFunction
        with TermLevel
        with WithField {
      def field: QueryField = in
    }
    object equals {
      implicit val decodeEquals: Decoder[equals] = buildBoostableDecoder("equals")(deriveDecoder[equals])
    }

    final case class exists private (value: QueryField, context: Option[QueryContext])
        extends QueryFunction
        with TermLevel
    object exists {
      implicit val decodeExists: Decoder[exists] = buildDecoder("exists")(deriveDecoder[exists])
    }

    final case class range private (in: QueryField.Single,
                                    value: RangeValue,
                                    context: Option[QueryContext],
                                    boost: Option[Float])
        extends QueryFunction
        with TermLevel
        with WithField {
      def field: QueryField.Single = in
    }
    object range {
      implicit val decodeRange: Decoder[range] = buildBoostableDecoder("range")(deriveDecoder[range])
    }

    final case class raw private (value: JsonObject, context: QueryContext)
        extends QueryFunction
        with WithContext {
      def ctx: QueryContext = context
    }
    object raw {
      implicit val decodeRaw: Decoder[raw] = buildDecoder("raw")(deriveDecoder[raw])
    }

    final case class bool private (value: QueryValue[QueryFunction], context: QueryContext)
        extends QueryFunction
        with WithContext {
      def ctx: QueryContext = context
    }
    object bool {
      // TODO: make it configurable (?)
      val MaxDepth = 25

      implicit val decodeBool: Decoder[bool] = buildDecoder[bool]("bool") {
        val decoder    = deriveDecoder[bool]
        val depthField = "_depth"

        Decoder.instance { c ⇒
          val depth = (for {
            parent ← c.up.focus
            parent ← parent.asObject
            depth  ← parent(depthField)
            depth  ← depth.as[Int].toOption
          } yield depth).getOrElse(1)

          // we start counting from 0,
          // which denotes implicit top-level bool query
          if (depth >= MaxDepth)
            Either.left(DecodingFailure(s"Max depth of $MaxDepth exceeded for a bool query", c.history))
          else
            decoder.tryDecode(c.withFocus(_.mapObject(_.add(depthField, Json.fromInt(depth + 1)))))
        }
      }
    }

    implicit val decodeQueryFunction: Decoder[QueryFunction] =
      Decoder[matches].map(identity[QueryFunction](_)) or
        Decoder[equals].map(identity[QueryFunction](_)) or
        Decoder[exists].map(identity[QueryFunction](_)) or
        Decoder[range].map(identity[QueryFunction](_)) or
        Decoder[raw].map(identity[QueryFunction](_)) or
        Decoder[bool].map(identity[QueryFunction](_))
  }

  final case class FCQuery(query: Option[NonEmptyList[QueryFunction]])
  object FCQuery {
    implicit val decodeFCQuery: Decoder[FCQuery] = {
      Decoder
        .decodeOption(
          Decoder.decodeNonEmptyList[QueryFunction] or
            Decoder[QueryFunction].map(NonEmptyList.of(_)))
        .map(FCQuery(_))
    }
  }
}
