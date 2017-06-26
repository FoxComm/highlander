package phoenix.services.activity

object MailTailored {
  case class SendSimpleMail(name: String, subject: String, email: String, html: String)
      extends ActivityBase[SendSimpleMail]
}
