package consumer.elastic

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.analyzers._
import com.sksamuel.elastic4s.mappings.FieldType._

package object mappings {
  val dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ||yyyy-MM-dd'T'HH:mm:ssZ"

  case object UppercaseTokenFilter extends TokenFilter {
    val name = "uppercase"
  }

  val autocompleteAnalyzer = CustomAnalyzerDefinition(
    "autocomplete",
    NGramTokenizer("autocomplete_tokenizer",
                   3,
                   20,
                   Seq("letter", "digit", "punctuation", "symbol", "whitespace")),
    LowercaseTokenFilter
  )

  val lowerCasedAnalyzer = CustomAnalyzerDefinition(
    "lower_cased",
    KeywordTokenizer("lower_cased_keyword_tokenizer"),
    LowercaseTokenFilter
  )

  val upperCasedAnalyzer = CustomAnalyzerDefinition(
    "upper_cased",
    KeywordTokenizer("upper_cased_keyword_tokenizer"),
    UppercaseTokenFilter
  )

  def address(name: String) = field(name).nested(
    field("address1", StringType).analyzer("autocomplete"),
    field("address2", StringType).analyzer("autocomplete"),
    field("city", StringType)
      .analyzer("autocomplete")
      .fields(field("raw", StringType).index("not_analyzed")),
    field("zip", StringType).index("not_analyzed"),
    field("region", StringType).index("not_analyzed"),
    field("country", StringType)
      .analyzer("autocomplete")
      .fields(field("raw", StringType).index("not_analyzed")),
    field("continent", StringType)
      .analyzer("autocomplete")
      .fields(field("raw", StringType).index("not_analyzed")),
    field("currency", StringType).index("not_analyzed")
  )
}
