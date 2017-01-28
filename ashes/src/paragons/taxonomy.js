// @flow

export type Taxonomy = {
  id?: number,
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
    hierarchical: isHierarchical,
    taxons: [],
  };
};
