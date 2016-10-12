package utils.seeds

import models.plugins.PluginSettings._
import models.plugins._
import org.json4s.JsonAST._
import payloads.PluginPayloads.RegisterPluginPayload
import services.plugins.PluginsManager
import utils.aliases._
import utils.db._

trait PluginSeeds {

  val accountNumber = SettingDef(name = "avatax_account_number",
                                 title = "AvaTax Account Number",
                                 `type` = PluginSettings.String)
  val companyCode = SettingDef(name = "avatax_company_code",
                               title = "AvaTax Company Code",
                               `type` = PluginSettings.String)
  val licenseKey = SettingDef(name = "avatax_license_key",
                              title = "AvaTax License Key",
                              `type` = PluginSettings.String)
  val serviceUrl = SettingDef(name = "avatax_service_url",
                              title = "AvaTax Service URL",
                              `type` = PluginSettings.String)
  val commitDocs = SettingDef(name = "avatax_commit_documents",
                              title = "Save and commit documents to AvaTax",
                              `type` = PluginSettings.Bool,
                              default = JBool(false))
  val logTransactions = SettingDef(name = "avatax_log_transactions",
                                   title = "Log AvaTax transactions in FoxCommerce",
                                   `type` = PluginSettings.Bool,
                                   default = JBool(false))

  val schema = Seq[SettingDef](accountNumber,
                               companyCode,
                               licenseKey,
                               serviceUrl,
                               commitDocs,
                               logTransactions)

  val settings = Map[String, JValue](
      accountNumber.name   → JString(""),
      companyCode.name     → JString(""),
      licenseKey.name      → JString(""),
      serviceUrl.name      → JString(""),
      commitDocs.name      → JBool(false),
      logTransactions.name → JBool(false)
  )
  val avalara = RegisterPluginPayload(name = "Avalara AvaTax",
                                      version = "1.0.0",
                                      description = "Sales tax stuff",
                                      apiHost = "0.0.0.0",
                                      apiPort = 9090,
                                      schemaSettings = Some(schema))

  def createAvalaraSettings()(implicit db: DB, ac: AC, ec: EC): DbResultT[Plugin] = {

    for {
      _      ← * <~ DbResultT.good(println("Inserting Avalara AvaTax settings"))
      plugin ← * <~ Plugins.create(Plugin.fromPayload(avalara).copy(settings = settings))
    } yield plugin
  }

}
