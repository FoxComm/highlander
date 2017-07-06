package foxcomm.agni.interpreter.es

import cats.data._
import foxcomm.agni._
import foxcomm.agni.dsl.sort._
import foxcomm.agni.interpreter.SortInterpreter
import org.elasticsearch.common.xcontent.{ToXContent, XContentBuilder}
import org.elasticsearch.search.sort.{SortBuilder, SortOrder}
import scala.annotation.compileTimeOnly
import scala.collection.mutable.ListBuffer

@SuppressWarnings(
  Array("org.wartremover.warts.MutableDataStructures", "org.wartremover.warts.NonUnitStatements"))
private[es] object ESSortInterpreter extends SortInterpreter[() ⇒ ?, NonEmptyList[SortBuilder]] {
  type State = () ⇒ NonEmptyList[SortBuilder]
  object State {
    def single(sort: ⇒ SortBuilder): State = () ⇒ NonEmptyList.of(sort)

    def apply(sorts: ⇒ List[SortBuilder]): State = () ⇒ NonEmptyList.fromListUnsafe(sorts)
  }

  private final case class RawSortBuilder(content: RawSortValue) extends SortBuilder {
    @compileTimeOnly("forbidden method to call")
    def missing(missing: scala.Any): SortBuilder = ???

    @compileTimeOnly("forbidden method to call")
    def order(order: SortOrder): SortBuilder = ???

    def toXContent(builder: XContentBuilder, params: ToXContent.Params): XContentBuilder = {
      content.eliminate(_.toMap.foreach {
        case (n, v) ⇒
          builder.rawField(n, v.toSmile)
      }, _.eliminate(name ⇒ {
        builder.startObject(name)
        builder.endObject()
      }, _.impossible))
      builder
    }
  }

  def apply(sfs: NonEmptyList[SortFunction]): State = State {
    sfs.foldLeft(ListBuffer.empty[SortBuilder])((acc, sf) ⇒ acc ++= eval(sf)().toList).toList
  }

  def rawF(sf: SortFunction.raw): State = State.single {
    RawSortBuilder(sf.value)
  }
}
