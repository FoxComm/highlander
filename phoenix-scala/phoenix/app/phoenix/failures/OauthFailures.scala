package phoenix.failures

import core.failures.Failure

object OauthFailures {

  case object InvalidEmailInUserInfo extends Failure {
    override def description: String = "Invalid email received from oauth provider"
  }

  case class CallbackResponseError(error: String) extends Failure {
    override def description: String = s"Can't process callback from oauth provider: $error"
  }

}
