
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
    const addingData = get(getState(), ['giftCards', 'adding']);

    const postData = {
      balance: addingData.balance,
      subTypeId: addingData.subTypeId,
      quantity: addingData.sendToCustomer ? addingData.customers.length : addingData.quantity,
      reasonId: 1, // @TODO: there only reason for now
      currency: 'USD'
    };

    return Api.post('/gift-cards', postData)
      .then(
        r => {
          console.log('r = ', r);
          dispatch(actions.fetch());
        },
        err => console.error(err)
      );
  };
}


export {
  reducer as default,
  actions
};
