// @flow
import type { Context } from 'paragons/object';

export type Taxon = {
  id?: number,
  parentId?: number,
  name: string,
};

export type TaxonResult = {
  id?: number,
  parentId?: number,
  name: string,
};
