package anthill.responses

final case class ProductResponse(
  id: Int,
  taxonomies: List[Taxonomy],
  productId: Int,
  retailPrice: String,
  salePrice: String,
  context: String,
  scope: String,
  skus: List[String],
  title: String,
  slug: String,
  tags: List[String],
  currency: String,
  albums: List[Album],
  description: String
)

final case class Taxonomy(taxonomy: String, taxons: List[List[String]])
final case class Album(name: String, images: List[Image], description: String)
final case class Image(alt: String, title: String, src: String, baseurl: Option[String] = None)
