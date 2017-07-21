// @flow
import _ from 'lodash';

import type { HasTaxons, HasESTaxons } from 'types/taxon';


export function getTaxonValue(obj: HasTaxons, name: string): ?string {
  const taxons = _.get(obj, 'taxons', []);
  const taxonomy = _.find(taxons, (taxonomyEntity) => {
    const taxonomyName = _.get(taxonomyEntity, 'attributes.name.v');
    return name === taxonomyName;
  });

  return _.get(taxonomy, ['taxons', 0, 'attributes', 'name', 'v']);
}

export function getESTaxonValue(obj: HasESTaxons, name: string): ?string {
  const taxons = _.get(obj, 'taxonomies', []);
  const taxonomy = _.find(taxons, {taxonomy: name});

  if (taxonomy) {
    return _.get(taxonomy, ['taxons', 0, 0]);
  }
}
