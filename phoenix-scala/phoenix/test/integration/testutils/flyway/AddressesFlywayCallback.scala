package testutils.flyway

import org.flywaydb.core.api.MigrationVersion
import slick.jdbc.PostgresProfile.api._

final class AddressesFlywayCallback extends FlywayMigrationsTest {
  val versionToTest = MigrationVersion.fromVersion("4.220")

  val insertPlugin =
    sql"""insert into plugins(name, version, description, is_disabled, api_host, api_port, settings,
             schema_settings, created_at, updated_at, deleted_at)
             values ('test_plugin', '123','', FALSE, null, null, '{}'::jsonb, '[]'::jsonb, now(), null,null)"""

  val pluginsDesc = sql"select COLUMN_NAME from INFORMATION_SCHEMA.COLUMNS where TABLE_NAME='plugins'"

  override def testBeforeMigration()(implicit db: Database): Unit = {
    pluginsDesc
      .as[String]
      .gimme mustNot contain("scope")

    insertPlugin.asUpdate.gimme
  }

  override def testAfterMigration()(implicit db: Database): Unit = {
    pluginsDesc
      .as[String]
      .gimme must contain("scope")

    sql"select scope from plugins where name = 'test_plugin'"
      .as[String]
      .gimme
      .head must === ("1")
  }

}
