package models

import utils.ModelWithIdParameter

final case class Store(id: Int, name: String, Configuration: StoreConfiguration) extends ModelWithIdParameter

