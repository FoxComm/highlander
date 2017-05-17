package phoenix.utils.seeds.generators
import scala.util.Random

object GeneratorUtils {
  def randomString(len: Int) = Random.alphanumeric.take(len).mkString.toLowerCase
}
