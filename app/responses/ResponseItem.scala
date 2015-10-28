package responses

trait ResponseItem {
  type ResponseSeq = ResponseWithFailuresAndMetadata[Seq[this.type]]
}