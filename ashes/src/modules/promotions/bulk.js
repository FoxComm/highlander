
import makeBulkActions, { createExportByIds } from '../bulk';
import { flow, filter, getOr, invoke, reduce, set } from 'lodash/fp';

const getCodes = (getState: Function, ids: Array<number>): Object => {
  return flow(
    invoke('promotions.list.currentSearch'),
    getOr([], 'results.rows'),
    filter(c => ids.indexOf(c.id) !== -1),
    reduce((obj, c) => set(c.id, c.id, obj), {})
  )(getState());
};

const exportByIds = createExportByIds(getCodes);

const { actions, reducer } = makeBulkActions('promotions.bulk', {
  exportByIds,
});

export {
  actions,
  reducer as default
};
