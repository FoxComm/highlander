package objectframework.activities

import objectframework.content.Content

case class ContentCreated(content: Content) extends Activity[ContentCreated]

object ContentCreated {
  def build(content: Content): ContentCreated =
    ContentCreated(content)
}
