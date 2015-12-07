
import _ from 'lodash';
import { get } from 'sprout-data';
import Api from '../../lib/api';
import makePagination from '../pagination';

const {
  reducer,
  actions: {
    fetch,
    setFetchParams,
    actionAddEntities
    }
  } = makePagination('/gift-cards', 'GIFT_CARDS');

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
      .then(results => {
        const giftCards = _.filter(results, {success: true}).map(entry => entry.giftCard);
        dispatch(actionAddEntities(giftCards));
      })
      .catch(err => console.error(err));
  };
}


export {
  reducer as default,
  fetch,
  setFetchParams
};
