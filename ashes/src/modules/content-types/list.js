import _ from 'lodash';
import { get } from 'sprout-data';
import Api from '../../lib/api';
import makeLiveSearch from '../live-search';
import searchTerms from './search-terms';

const { reducer, actions } = makeLiveSearch(
  'contentTypes.list',
  searchTerms,
  'content_types_search_view/_search',
  'contentTypesScope',
  {
    initialState: { sortBy: '-createdAt' }
  }
);

export function createContentType() {
  return (dispatch, getState) => {
    const addingData = get(getState(), ['contentTypes', 'adding', 'contentType']);

    const postData = {
      balance: addingData.balance,
      quantity: addingData.quantity,
      subTypeId: addingData.subTypeId,
      reasonId: addingData.reasonId,
      currency: 'USD'
    };

    return Api.post('/content-types/bulk', postData)
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
