package cms

case class Content(id: Int, commitId: Int, contextId: Int, attributes: Map[String, ContentType])
