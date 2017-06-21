package testutils

import org.json4s.jackson.parseJson
import org.scalatest.AppendedClues
import phoenix.utils.aliases.{SF, SL}
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.api._
import core.db.ExPostgresDriver.api._

trait SearchViewTestBase
    extends IntegrationTestBase
    with PhoenixAdminApi
    with DefaultJwtAdminAuth
    with ApiFixtures
    with AppendedClues {

  // Case class that holds all search view fields
  type SearchViewResult

  // Search view name
  def searchViewName: String

  // Search key name, e.g. id/code/reference_number/whatever
  def searchKeyName: String

  /*
   * I believe we should test views only by fetching a single row.
   * Querying for all items in search view can lead to tests that might occasionally fail depending on previous
   * search view table state (this can and will happen if we run ITs in parallel).
   * If you think something should be tested against all search view results, reach out to Anna before implementing.
   */
  // queryParam is value of id/code/reference_number etc
  def findOne(queryParam: AnyVal)(implicit sl: SL,
                                  sf: SF,
                                  mf: Manifest[SearchViewResult]): Option[SearchViewResult] = {
    val results = queryView(queryParam).toSeq.flatten
    (results.size must be <= 1) withClue s"Too many search view results for $searchKeyName=$queryParam!\n"
    results.headOption
  } withClue originalSourceClue

  def viewOne(queryParam: AnyVal)(implicit sl: SL, sf: SF, mf: Manifest[SearchViewResult]): SearchViewResult =
    findOne(queryParam).value withClue originalSourceClue

  // As the name suggests, use this to debug view tests. Only to debug.
  def DEBUG_rawViewResult(implicit sl: SL, sf: SF, mf: Manifest[SearchViewResult]): Vector[String] =
    sql"select array_to_json(array_agg(sv)) from #$searchViewName as sv".as[String].gimme

  private def queryView(queryParam: AnyVal)(implicit sl: SL,
                                            sf: SF,
                                            mf: Manifest[SearchViewResult]): Option[Seq[SearchViewResult]] = {
    // Select all search view rows as JSON array, just because it's easier to implement it this way rather than
    // wreste with complex Slick's type definitions
    val query =
      sql"select array_to_json(array_agg(sv)) from #$searchViewName as sv where sv.#$searchKeyName=#$queryParam"
        .as[String]

    Option(query.gimme.onlyElement).map { jsonString ⇒
      // .camelizeKeys allows to convert snake_cased search view table to camelCased Scala case class. Sweet!
      parseJson(jsonString).camelizeKeys
        .extract[Seq[SearchViewResult]]
        .withClue(s"Failed to parse JSON, raw data was $jsonString")
    }
  } withClue originalSourceClue
}
