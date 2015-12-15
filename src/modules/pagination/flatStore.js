
import _ from 'lodash';
import {createActions, paginateReducer} from './v2';

const makePagination = (dataNamespace) => {
  return (makeUrl, moduleReducer, updateBehaviour) => {

    return {
      reducer: paginateReducer(dataNamespace, moduleReducer, updateBehaviour),
      actions: createActions(dataNamespace, () => [], (identity, payload) => payload)(makeUrl),
    };
  };
};

export default makePagination;
