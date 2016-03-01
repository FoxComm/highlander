
import _ from 'lodash';
import { get } from 'sprout-data';
import Api from '../../lib/api';
import makeLiveSearch from '../live-search';
import searchTerms from './search-terms';

const { reducer, actions } = makeLiveSearch(
  'giftCards.list',
  searchTerms,
  'gift_cards_search_view/_search',
  'giftCardsScope'
);

export function createGiftCard() {
  return (dispatch, getState) => {
    const addingData = get(getState(), ['giftCards', 'adding', 'giftCard']);

    const quantity = addingData.sendToCustomer ? addingData.customers.length : addingData.quantity;

    const postData = {
      balance: addingData.balance,
      subTypeId: addingData.subTypeId,
      reasonId: 1, // @TODO: there only reason for now
      currency: 'USD'
    };
    if (quantity > 1) {
      postData.quantity = quantity;
    }

    return Api.post('/gift-cards', postData)
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
