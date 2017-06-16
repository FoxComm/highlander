/* @flow */

import _ from 'lodash';
import makeBulkActions, { createExportByIds } from '../bulk';

const getExportedIds = (getState: Function, ids: Array<number>): Object => {
  return _.reduce(ids, (obj, entry) => _.set(obj, entry, entry), {});
};

const exportByIds = createExportByIds(getExportedIds);

const { actions, reducer } = makeBulkActions('promotions.bulk', {
  exportByIds,
});

export {
  actions,
  reducer as default
};
