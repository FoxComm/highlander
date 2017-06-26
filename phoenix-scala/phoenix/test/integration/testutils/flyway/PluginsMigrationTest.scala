package testutils.flyway

import slick.jdbc.PostgresProfile.api._

/*
    In this test we gonna check whether `plugins.scope` column was properly added and default value assigned as it should be
 */
case object PluginsMigrationTest extends FlywayMigrationsTest("4.220") {

  val pluginsDesc = sql"select column_name from INFORMATION_SCHEMA.COLUMNS where table_name='plugins'"

  override def testBeforeMigration(implicit db: Database): Unit = {
    pluginsDesc
      .as[String]
      .gimme mustNot contain("scope")

    sql"""insert into plugins(name, version, description, is_disabled, created_at)
             values ('test_plugin', '123','', false, now())""".asUpdate.gimme
  }

  override def testAfterMigration(implicit db: Database): Unit = {
    pluginsDesc
      .as[String]
      .gimme must contain("scope")

    sql"select scope from plugins where name = 'test_plugin'"
      .as[String]
      .gimme
      .head must === ("1")
  }

}
