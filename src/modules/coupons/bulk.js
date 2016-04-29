
import makeBulkActions from '../discounts';

const { actions, reducer } = makeBulkActions('coupon');

export {
  actions,
  reducer as default
};
