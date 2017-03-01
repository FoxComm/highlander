// @flow

//libs
import { dissoc } from 'sprout-data';

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

export const duplicateTaxonomy = (taxonomy: Taxonomy): Taxonomy => {
  const cleared = dissoc(taxonomy, 'id');
  return cleared;
};
