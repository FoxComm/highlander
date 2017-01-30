// @flow
import type { Context } from 'paragons/object';

export type Taxonomy = {
  id?: number,
  context: Context,
  hierarchical: boolean,
  attributes: Attributes,
  taxons: Array<Object>,
};

export type TaxonomyResult = {
  id: number,
  taxonomyId: number,
  name: string,
  context: string,
  type: string,
  valuesCount: number,
  activeFrom: ?string,
  activeTo: ?string,
  archivedAt: ?string,
};

export const createEmptyTaxonomy = (context: string, isHierarchical: boolean): Taxonomy => {
  return {
    attributes: {
      name: { t: 'string', v: '' },
      description: { t: 'richText', v: '' },
    },
    context: {
      name: context,
    },
    hierarchical: isHierarchical,
    taxons: [],
  };
};
