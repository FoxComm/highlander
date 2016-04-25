import akka.http.scaladsl.model.StatusCodes

import org.scalatest.mock.MockitoSugar
import responses.{TheResponse, PaginationMetadata, ResponseItem}
import util.IntegrationTestBase
import utils.http.CustomDirectives

trait SortingAndPaging[T <: ResponseItem] extends MockitoSugar { this: IntegrationTestBase with HttpSupport ⇒

  // the API
  def numOfResults = 30
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

  implicit class TheResponseMetadataChecks[A <: AnyRef](resp: TheResponse[Seq[A]]) {
    def checkSortingAndPagingMetadata(sortBy: String, from: Int, size: Int, resultSize: Int, total:
        Option[Int] = None): TheResponse[Seq[A]] = {
      resp.checkSortingMetadata(sortBy).checkPagingMetadata(from, size, resultSize, total)
      resp
    }

    def checkPagingMetadata(from: Int, size: Int, resultSize: Int, total: Option[Int] = None): TheResponse[Seq[A]] = {
      resp.result.size must === (resultSize)
      resp.pagination must be ('defined)
      resp.pagination.value.from must be ('defined)
      resp.pagination.value.from.value must === (from)
      resp.pagination.value.size must be ('defined)
      resp.pagination.value.size.value must === (size)
      resp.pagination.value.pageNo must be ('defined)
      resp.pagination.value.pageNo.value must === ((from / size) + 1)
      resp.pagination.value.total must be ('defined)
      total.foreach(resp.pagination.value.total.value must === (_))
      resp
    }

    def checkSortingMetadata(sortBy: String): TheResponse[Seq[A]] = {
      resp.sorting must be ('defined)
      resp.sorting.value.sortBy must be ('defined)
      resp.sorting.value.sortBy.value must === (sortBy)
      resp
    }
  }

  "supports sorting and paging" - {
    pending

    "sort by a column without paging" in new SortingAndPagingFixture {
      val response = GET(s"$uriPrefix?sortBy=$sortColumnName")
      response.status must === (StatusCodes.OK)
      val respWithMetadata = response.withResultTypeOf[Seq[T]]
      respWithMetadata.result must === (itemsSorted)

      respWithMetadata.sorting must be ('defined)
      respWithMetadata.sorting.value.sortBy must be ('defined)
      respWithMetadata.sorting.value.sortBy.value must === (sortColumnName)
      respWithMetadata.pagination must === (Some(PaginationMetadata(Some(0),Some(CustomDirectives.DefaultPageSize),
        Some(1),Some(30))))
    }

    "sort by a column with paging #1" in new SortingAndPagingFixture {
      val response = GET(s"$uriPrefix?sortBy=$sortColumnName&from=12&size=6")

      response.status must === (StatusCodes.OK)
      val respWithMetadata = response.withResultTypeOf[Seq[T]]
      respWithMetadata.result must === (itemsSorted.drop(12).take(6))

      respWithMetadata.checkSortingAndPagingMetadata(sortColumnName, 12, 6, 6, Some(30))
    }

    "sort by a column with paging #2" in new SortingAndPagingFixture {
      val responseList = GET(s"$uriPrefix?sortBy=$sortColumnName&from=999&size=3")

      responseList.status must === (StatusCodes.OK)
      responseList.ignoreFailuresAndGiveMe[Seq[T]] must === (Seq.empty)
    }

    "sort by a column with paging #3" in new SortingAndPagingFixture {
      val responseList = GET(s"$uriPrefix?sortBy=$sortColumnName&from=0&size=999")

      responseList.status must === (StatusCodes.OK)
      responseList.ignoreFailuresAndGiveMe[Seq[T]] must === (itemsSorted)
    }

    "sort by a column in a reverse order with paging" in new SortingAndPagingFixture {
      val responseList = GET(s"$uriPrefix?sortBy=-$sortColumnName&from=12&size=6")

      responseList.status must === (StatusCodes.OK)
      responseList.ignoreFailuresAndGiveMe[Seq[T]] must === (itemsSorted.reverse.drop(12).take(6))
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

    "error on invalid sortBy param" in new SortingAndPagingFixture {
      val responseList = GET(s"$uriPrefix?sortBy=3242&from=3&size=10")

      responseList.status must === (StatusCodes.BadRequest)
    }
  }


}
