import makePagination from '../pagination';

const {reducer, actions: {fetch, setFetchParams}} = makePagination('/customers', 'CUSTOMERS');

export default reducer;

export {
  fetch,
  setFetchParams
};
