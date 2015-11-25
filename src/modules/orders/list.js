
import makePagination from '../pagination';

const {reducer, actions: {fetch, setFetchParams}} = makePagination('/orders', 'ORDERS');

export default reducer;

export {
  fetch,
  setFetchParams
};
