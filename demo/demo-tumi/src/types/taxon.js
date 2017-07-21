
import type { Attributes } from './attributes';

export type Taxon = {
  attributes: Attributes;
}

export type HasTaxons = {
  taxons: Array<Taxon>,
}

export type ESTaxon = {
  taxonomy: string,
  taxons: Array<Array<string>>
}

export type HasESTaxons = {
  taxonomies: Array<ESTaxon>
}
