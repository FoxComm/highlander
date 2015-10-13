import akka.http.scaladsl.model.StatusCodes

import org.scalatest.mock.MockitoSugar
import responses.ResponseItem
import util.IntegrationTestBase

trait SortingAndPaging[T <: ResponseItem] { this: IntegrationTestBase with HttpSupport with MockitoSugar ⇒

  // the API
  def uriPrefix: String
  def sortColumnName: String
  def responseItems: IndexedSeq[T]
  def responseItemsSort(items: IndexedSeq[T]): IndexedSeq[T]
  def mf: scala.reflect.Manifest[T]



  private implicit def manifest = mf
  import Extensions._

  trait SortingAndPagingFixture {
    val items: IndexedSeq[T] = responseItems
    val itemsSorted: IndexedSeq[T] = responseItemsSort(items)
  }

  "supports sorting and paging" - {

    "sort by id with paging" in new SortingAndPagingFixture {
      val responseList = GET(s"$uriPrefix?pageSize=5&pageNo=3&sortBy=id")

      responseList.status must === (StatusCodes.OK)
      responseList.as[Seq[T]] must === (items.drop(10).take(5))
    }

    "sort by a column without paging" in new SortingAndPagingFixture {
      val responseList = GET(s"$uriPrefix?sortBy=$sortColumnName")

      responseList.status must === (StatusCodes.OK)
      responseList.as[Seq[T]] must === (itemsSorted)
    }

    "sort by a column with paging #1" in new SortingAndPagingFixture {
      val responseList = GET(s"$uriPrefix?sortBy=$sortColumnName&pageNo=3&pageSize=6")

      responseList.status must === (StatusCodes.OK)
      responseList.as[Seq[T]] must === (itemsSorted.drop(12).take(6))
    }

    "sort by a column with paging #2" in new SortingAndPagingFixture {
      val responseList = GET(s"$uriPrefix?sortBy=$sortColumnName&pageNo=3&pageSize=999")

      responseList.status must === (StatusCodes.OK)
      responseList.as[Seq[T]] must === (Seq.empty)
    }

    "sort by a column with paging #3" in new SortingAndPagingFixture {
      val responseList = GET(s"$uriPrefix?sortBy=$sortColumnName&pageNo=1&pageSize=999")

      responseList.status must === (StatusCodes.OK)
      responseList.as[Seq[T]] must === (itemsSorted)
    }

    "error on invalid pageNo param #1" in new SortingAndPagingFixture {
      val responseList = GET(s"$uriPrefix?sortBy=$sortColumnName&pageNo=sdfgdsg&pageSize=10")

      responseList.status must === (StatusCodes.BadRequest)
    }

    "error on invalid pageNo param #2" in new SortingAndPagingFixture {
      val responseList = GET(s"$uriPrefix?sortBy=$sortColumnName&pageNo=-10&pageSize=10")

      responseList.status must === (StatusCodes.InternalServerError)
    }

    "error on invalid pageNo param #3" in new SortingAndPagingFixture {
      val responseList = GET(s"$uriPrefix?sortBy=$sortColumnName&pageNo=0&pageSize=10")

      responseList.status must === (StatusCodes.InternalServerError)
    }

    "error on invalid pageSize param #1" in new SortingAndPagingFixture {
      val responseList = GET(s"$uriPrefix?sortBy=$sortColumnName&pageNo=10&pageSize=sdfgdsg")

      responseList.status must === (StatusCodes.BadRequest)
    }

    "error on invalid pageSize param #2" in new SortingAndPagingFixture {
      val responseList = GET(s"$uriPrefix?sortBy=$sortColumnName&pageNo=1&pageSize=-10")

      responseList.status must === (StatusCodes.InternalServerError)
    }

    "error on invalid pageSize param #3" in new SortingAndPagingFixture {
      val responseList = GET(s"$uriPrefix?sortBy=$sortColumnName&pageNo=1&pageSize=0")

      responseList.status must === (StatusCodes.OK)
      responseList.as[Seq[T]] must === (Seq.empty)
    }

    "ignore invalid sortBy param" in new SortingAndPagingFixture {
      val responseList = GET(s"$uriPrefix?sortBy=3242&pageNo=3&pageSize=10")

      responseList.status must === (StatusCodes.OK)
      responseList.as[Seq[T]] must === (items.drop(20).take(10))
    }
  }


}
