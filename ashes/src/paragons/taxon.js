// @flow

//libs
import { dissoc } from 'sprout-data';

export const createEmptyTaxon = () => {
  return {
    attributes: {
      name: { t: 'string', v: '' },
      description: { t: 'richText', v: '' },
    },
    location: {},
  };
};

export const duplicateTaxon = (taxon: Taxon) => {
  const cleared = dissoc(taxon, 'id', 'taxonomyId');
  return cleared;
};
