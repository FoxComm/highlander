package models

import utils.Model

final case class Store(id: Int, name: String, Configuration: StoreConfiguration) extends Model

