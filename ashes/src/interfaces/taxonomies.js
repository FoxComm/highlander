declare type TaxonomyDraft = {
  context: Context,
  hierarchical: boolean,
  attributes: Attributes,
  taxons: Array<Object>,
};

declare type Taxonomy = TaxonomyDraft & {
  id: number,
}

declare type TaxonomyResult = {
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

declare type TaxonDraft = {
  parentId?: number,
  name: string,
  attributes: Attributes
};

declare type Taxon = TaxonDraft & {
  id: number,
};

declare type TaxonResult = {
  id?: number,
  parentId?: number,
  name: string,
};
