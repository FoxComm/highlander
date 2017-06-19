import phoenix.models.location.{Addresses, Region, Regions}
import testutils.{DefaultJwtAdminAuth, HttpSupport, IntegrationTestBase}
import slick.jdbc.PostgresProfile.api._
import testutils.apis.{PhoenixAdminApi, PhoenixPublicApi}
import testutils.fixtures.BakedFixtures
import testutils.fixtures.api.ApiFixtureHelpers

class TargetedMigrationIntegrationTest extends IntegrationTestBase {

  "make a good use out of targeted migration" - {

    "test it" in {
      val v = sql"select * from test_table".as[(Int, String)].gimme
    }

    "show schema" in {
      val schema = sql"select * from schema_version".as[(Int, String, String)].gimme
    }

  }
}
