import makePagination from '../pagination';

const {reducer, actions: {fetch, setFetchParams}} = makePagination('/gift-cards', 'GIFT_CARDS');

export default reducer;

export {
  fetch,
  setFetchParams
};
