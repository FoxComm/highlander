/* @flow */

import createStore from 'lib/store-creator';
import { reducers, createExportByIds } from '../bulk';
import { getPropsByIds } from 'modules/bulk-export/helpers';

const getTaxonomies = (getState: Function, ids: Array<number>) => {
  return getPropsByIds('taxonomies', ids, ['taxonomyId', 'context'], getState());
};

const exportByIds = createExportByIds(getTaxonomies);

const { actions, reducer } = createStore({
  path: 'taxonomies.bulk',
  actions: {
    exportByIds,
  },
  reducers,
});

export {
  actions,
  reducer as default
};
