// @flow

//libs
import { dissoc } from 'sprout-data';

export const createEmptyTaxon = () => {
  return {
    attributes: {
      name: { t: 'string', v: '' },
      description: { t: 'richText', v: '' }
    }
  };
};

export const duplicateTaxon = (taxon) => {
  const cleared = dissoc(taxon, 'id', 'taxonomyId');
  return cleared;
};
