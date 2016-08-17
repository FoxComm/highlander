package utils.apis

import utils.FoxConfig._

trait AvalaraApi {

}

class AvalaraApi extends AvalaraApi {
  private def getConfig(): (String, String, String, String) = {
    val url = config.getOptString("avalara.url")
    val account = config.getOptString("avalara.account")
    val licence = config.getOptString("avalara.licence")
    val profile = config.getOptString("avalara.profile")
  }
}
