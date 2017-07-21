export type Attribute = {
  t: string,
  v: any,
};

export type Attributes = { [key:string]: Attribute };

import type { HasTaxons } from './taxon';

export type Image = {
  alt?: string,
  src: string,
  title?: string,
};

export type Album = {
  name: string,
  images: Array<Image>,
};

export type Sku = HasTaxons & {
  id?: number,
  attributes: Attributes,
  albums: Array<Album>,
};
