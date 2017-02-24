// @flow

export const createEmptyTaxonomy = (context: string, isHierarchical: boolean): TaxonomyDraft => {
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
