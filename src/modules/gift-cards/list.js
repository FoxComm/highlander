
import _ from 'lodash';
import { get } from 'sprout-data';
import Api from '../../lib/api';
import makePagination from '../pagination';
import makeLiveSearch from '../live-search';
import searchTerms from './search-terms';


const { actionAddEntities } = makePagination('/gift-cards', 'GIFT_CARDS');

const { reducer, actions } = makeLiveSearch(
  'giftCards', 
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
        results => {
          const giftCards = _.filter(results, {success: true}).map(entry => entry.giftCard);
          dispatch(actionAddEntities(giftCards));
        },
        err => console.error(err)
      );
  };
}


export {
  reducer as default,
  actions
};
