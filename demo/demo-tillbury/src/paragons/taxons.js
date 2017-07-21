// @flow
import _ from 'lodash';

import type { HasTaxons } from 'types/taxon';

export function getTaxonValue(obj: HasTaxons, name: string): ?string {
  const taxons = _.get(obj, 'taxons', []);
  const taxonomy = _.find(taxons, (taxonomyEntity) => {
    const taxonomyName = _.get(taxonomyEntity, 'attributes.name.v');
    return name === taxonomyName;
  });

  return _.get(taxonomy, ['taxons', 0, 'attributes', 'name', 'v']);
}
