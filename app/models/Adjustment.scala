package models

import utils.Model

final case class Adjustment(id: Int, amount: Int, sourceId: Int, sourceType: String, reason: String) extends Model

