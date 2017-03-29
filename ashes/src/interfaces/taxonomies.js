declare type TaxonomyDraft = {
  context: Context,
  hierarchical: boolean,
  attributes: Attributes,
  taxons: TaxonsTree,
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

declare type TaxonLocation = {
  parent?: number,
  location?: number,
}
declare type TaxonDraft = {
  parentId: ?number,
  location?: TaxonLocation,
  attributes: Attributes
};

declare type Taxon = TaxonDraft & {
  id: number,
  taxonomyId: number,
};

declare type TaxonResult = {
  id: number,
  name: string,
  location?: TaxonLocation,
};

declare type TaxonNode = {
  children: TaxonsTree,
  node: Taxon,
};

declare type TaxonsTree = Array<TaxonNode>;
