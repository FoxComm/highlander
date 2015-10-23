import akka.http.scaladsl.model.StatusCodes

import org.scalatest.mock.MockitoSugar
import responses.ResponseItem
import util.IntegrationTestBase
import utils.Http.HttpResponseWithMetadata

trait SortingAndPaging[T <: ResponseItem] extends MockitoSugar { this: IntegrationTestBase with HttpSupport ⇒

  // the API
  def uriPrefix: String
  def sortColumnName: String
  def beforeSortingAndPaging(): Unit = {}
  def responseItems: IndexedSeq[T]
  def responseItemsSort(items: IndexedSeq[T]): IndexedSeq[T]
  def mf: scala.reflect.Manifest[T]



  private implicit def manifest = mf
  import Extensions._

  trait SortingAndPagingFixture {
    beforeSortingAndPaging()
    val items: IndexedSeq[T] = responseItems
    val itemsSorted: IndexedSeq[T] = responseItemsSort(items)
  }

  "supports sorting and paging" - {

    "sort by a column without paging" in new SortingAndPagingFixture {
      val response = GET(s"$uriPrefix?sortBy=$sortColumnName")
      response.status must === (StatusCodes.OK)
      val respWithMetadata = response.as[HttpResponseWithMetadata[Seq[T]]]
      respWithMetadata.result must === (itemsSorted)

      respWithMetadata.sorting must be ('defined)
      respWithMetadata.sorting.value.sortBy must be ('defined)
      respWithMetadata.sorting.value.sortBy.value must === (sortColumnName)
      respWithMetadata.pagination must === (None)
    }

    "sort by a column with paging #1" in new SortingAndPagingFixture {
      val response = GET(s"$uriPrefix?sortBy=$sortColumnName&from=12&size=6")

      response.status must === (StatusCodes.OK)
      val respWithMetadata = response.as[HttpResponseWithMetadata[Seq[T]]]
      respWithMetadata.result must === (itemsSorted.drop(12).take(6))

      respWithMetadata.sorting must be ('defined)
      respWithMetadata.sorting.value.sortBy must be ('defined)
      respWithMetadata.sorting.value.sortBy.value must === (sortColumnName)
      respWithMetadata.pagination must be ('defined)
      respWithMetadata.pagination.value.from must be ('defined)
      respWithMetadata.pagination.value.from.value must === (12)
      respWithMetadata.pagination.value.size must be ('defined)
      respWithMetadata.pagination.value.size.value must === (6)
      respWithMetadata.pagination.value.pageNo must be ('defined)
      respWithMetadata.pagination.value.pageNo.value must === (3)
      respWithMetadata.pagination.value.totalPages must be ('defined)
      respWithMetadata.pagination.value.totalPages.value must === (5)
    }

    "sort by a column with paging #2" in new SortingAndPagingFixture {
      val responseList = GET(s"$uriPrefix?sortBy=$sortColumnName&from=999&size=3")

      responseList.status must === (StatusCodes.OK)
      responseList.as[HttpResponseWithMetadata[Seq[T]]].result must === (Seq.empty)
    }

    "sort by a column with paging #3" in new SortingAndPagingFixture {
      val responseList = GET(s"$uriPrefix?sortBy=$sortColumnName&from=0&size=999")

      responseList.status must === (StatusCodes.OK)
      responseList.as[HttpResponseWithMetadata[Seq[T]]].result must === (itemsSorted)
    }

    "sort by a column in a reverse order with paging" in new SortingAndPagingFixture {
      val responseList = GET(s"$uriPrefix?sortBy=-$sortColumnName&from=12&size=6")

      responseList.status must === (StatusCodes.OK)
      responseList.as[HttpResponseWithMetadata[Seq[T]]].result must === (itemsSorted.reverse.drop(12).take(6))
    }

    "error on invalid from param #1" in new SortingAndPagingFixture {
      val responseList = GET(s"$uriPrefix?sortBy=$sortColumnName&from=sdfgdsg&size=10")

      responseList.status must === (StatusCodes.BadRequest)
    }

    "error on invalid from param #2" in new SortingAndPagingFixture {
      val responseList = GET(s"$uriPrefix?sortBy=$sortColumnName&from=-10&size=10")

      responseList.status must === (StatusCodes.BadRequest)
    }

    "error on invalid size param #1" in new SortingAndPagingFixture {
      val responseList = GET(s"$uriPrefix?sortBy=$sortColumnName&from=10&size=sdfgdsg")

      responseList.status must === (StatusCodes.BadRequest)
    }

    "error on invalid size param #2" in new SortingAndPagingFixture {
      val responseList = GET(s"$uriPrefix?sortBy=$sortColumnName&from=1&size=-10")

      responseList.status must === (StatusCodes.BadRequest)
    }

    "error on invalid size param #3" in new SortingAndPagingFixture {
      val responseList = GET(s"$uriPrefix?sortBy=$sortColumnName&from=1&size=0")

      responseList.status must === (StatusCodes.BadRequest)
    }

    "ignore invalid sortBy param" in new SortingAndPagingFixture {
      val responseList = GET(s"$uriPrefix?sortBy=3242&from=3&size=10")

      responseList.status must === (StatusCodes.OK)
    }
  }


}
