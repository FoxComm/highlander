
import makeBulkActions from '../bulk';

const { actions, reducer } = makeBulkActions('promotions.bulk');

export {
  actions,
  reducer as default
};
