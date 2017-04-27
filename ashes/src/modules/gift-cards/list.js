
import _ from 'lodash';
import { get } from 'sprout-data';
import Api from '../../lib/api';
import makeLiveSearch from '../live-search';
import searchTerms from './search-terms';

const { reducer, actions } = makeLiveSearch(
  'giftCards.list',
  searchTerms,
  'gift_cards_search_view/_search',
  'giftCardsScope',
  {
    initialState: { sortBy: '-createdAt' }
  }
);

export function createGiftCard() {
  return (dispatch, getState) => {
    const addingData = get(getState(), ['giftCards', 'adding', 'giftCard']);

    const quantity = addingData.sendToCustomer ? addingData.customers.length : addingData.quantity;

    const postData = {
      balance: addingData.balance,
      quantity: addingData.quantity,
      subTypeId: addingData.subTypeId,
      reasonId: addingData.reasonId,
      currency: 'USD'
    };

    return Api.post('/gift-cards/bulk', postData)
      .then(
        response => {
          dispatch(actions.fetch());
          return response;
        },
        err => {
          console.error(err);
          return err;
        }
      );
  };
}


export {
  reducer as default,
  actions
};
