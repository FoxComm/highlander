
import type { Attributes } from './attributes';

export type Taxon = {
  attributes: Attributes;
}

export type HasTaxons = {
  taxons: Array<Taxon>,
}
