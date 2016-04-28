
import makeBulkActions from '../discounts';

const { actions, reducer } = makeBulkActions('promotion');

export {
  actions,
  reducer as default
};
