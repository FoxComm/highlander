package utils.seeds

import models.plugins._
import payloads.PluginPayloads.RegisterPluginPayload
import utils.aliases._
import utils.db._

trait PluginSeeds {

  def createAvalaraSettings()(implicit db: DB, ac: AC, ec: EC): DbResultT[Plugin] = {
    val avalara = RegisterPluginPayload(name = "Avalara AvaTax",
                                        version = "1.0.0",
                                        description = "Sales tax stuff",
                                        apiHost = "0.0.0.0",
                                        apiPort = 9090,
                                        schemaSettings = None)
    for {
      _      ← * <~ DbResultT.good(println("Inserting Avalara AvaTax settings"))
      plugin ← * <~ Plugins.create(Plugin.fromPayload(avalara))
    } yield plugin
  }

}
