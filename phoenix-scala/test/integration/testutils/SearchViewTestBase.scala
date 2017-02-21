package testutils

import org.json4s.jackson.parseJson
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.api._
import utils.MockedApis
import utils.db.ExPostgresDriver.api._

trait SearchViewTestBase
    extends IntegrationTestBase
    with PhoenixAdminApi
    with AutomaticAuth
    with ApiFixtures
    with MockedApis {

  // Case class that holds all search view fields
  type SearchViewResult
  // Search view name
  def searchViewName: String
  // Search key name, e.g. id/code/reference_number/whatever
  def searchKeyName: String

  /*
   * There is no `findAllInSearchView` available because I believe we should test against fetching a single row.
   * Querying for all items in search view can lead to tests that might occasionally fail depending on previous
   * search view table state (this can and will happen if we run ITs in parallel).
   * If you think something should be tested against all search view results, reach out to Anna before implementing.
   */
  // Identifier is value of id/code/reference_number etc
  def findOneInSearchView(identifier: AnyVal)(
      implicit mf: Manifest[SearchViewResult]): SearchViewResult = {

    import OnlyElement._
    // Select all search view rows as JSON array. Just because it's easier to implement it this way rather than
    // wreste with complex Slick's type definitions
    val query =
      sql"select array_to_json(array_agg(sv)) from #$searchViewName as sv where sv.#$searchKeyName=#$identifier"
        .as[String]

    // The only element is the JSON array
    val jsonString = query.gimme.onlyElement
    withClue("Query result was empty. Slick returns Vector(null) instead of empty Vector.\n") {
      jsonString must not be null
    }

    // .camelizeKeys allows to convert snake_cased search view to camelCased Scala case class. Sweet!
    parseJson(jsonString).camelizeKeys.extract[Seq[SearchViewResult]].onlyElement
  }

}
