export type TaxonomyDraft = {
  context: Context,
  hierarchical: boolean,
  attributes: Attributes,
  taxons: Array<Object>,
};

export type Taxonomy = TaxonomyDraft & {
  id: number,
}

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
