import makePagination from '../pagination';

const {reducer, fetch} = makePagination('/orders', 'ORDERS');

export default reducer;

export {
  fetch,
};
