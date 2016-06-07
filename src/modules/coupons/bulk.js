
import makeBulkActions from '../bulk';

const { actions, reducer } = makeBulkActions('coupons.bulk');

export {
  actions,
  reducer as default
};
