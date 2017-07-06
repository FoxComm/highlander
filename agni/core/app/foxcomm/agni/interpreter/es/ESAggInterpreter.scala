package foxcomm.agni.interpreter.es

import cats.data._
import foxcomm.agni._
import foxcomm.agni.dsl.aggregations._
import foxcomm.agni.interpreter.AggregationInterpreter
import io.circe.JsonObject
import org.elasticsearch.common.xcontent.{ToXContent, XContentBuilder}
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder
import scala.collection.mutable.ListBuffer

@SuppressWarnings(
  Array("org.wartremover.warts.MutableDataStructures", "org.wartremover.warts.NonUnitStatements"))
private[es] object ESAggInterpreter
    extends AggregationInterpreter[() ⇒ ?, NonEmptyList[AbstractAggregationBuilder]] {
  type State = () ⇒ NonEmptyList[AbstractAggregationBuilder]
  object State {
    def single(agg: ⇒ AbstractAggregationBuilder): State = () ⇒ NonEmptyList.of(agg)

    def apply(aggs: ⇒ List[AbstractAggregationBuilder]): State = () ⇒ NonEmptyList.fromListUnsafe(aggs)
  }

  private final case class RawAggBuilder(name: String,
                                         tpe: String,
                                         meta: Option[JsonObject],
                                         content: JsonObject)
      extends AbstractAggregationBuilder(name, tpe) {
    def toXContent(builder: XContentBuilder, params: ToXContent.Params): XContentBuilder = {
      builder.startObject(name)

      meta.foreach { m ⇒
        builder.startObject("meta")
        m.toMap.foreach {
          case (n, v) ⇒
            builder.rawField(n, v.toSmile)
        }
        builder.endObject()
      }

      builder.startObject(tpe)
      content.toMap.foreach {
        case (n, v) ⇒
          builder.rawField(n, v.toSmile)
      }
      builder.endObject()

      builder.endObject()
    }
  }

  def apply(afs: NonEmptyList[AggregationFunction]): State = State {
    afs.foldLeft(ListBuffer.empty[AbstractAggregationBuilder])((acc, sf) ⇒ acc ++= eval(sf)().toList).toList
  }

  def rawF(af: AggregationFunction.raw): State = State.single {
    RawAggBuilder(name = af.name, tpe = af.tpe, meta = af.meta, content = af.value)
  }
}
