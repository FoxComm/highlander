// @flow

export type Taxonomy = {
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

export const createEmptyTaxonomy = (context: string, type: string): Taxonomy => {
  return {
    id: 0,
    taxonomyId: 0,
    name: '',
    valuesCount: 0,
    activeFrom: null,
    activeTo: null,
    archivedAt: null,
    context,
    type,
  };
};
