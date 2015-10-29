package responses

trait ResponseItem {
  type ResponseMetadataSeq = ResponseWithFailuresAndMetadata[Seq[this.type]]
}