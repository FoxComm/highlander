import _ from 'lodash';
import Api from '../../lib/api';
import { createAction, createReducer } from 'redux-act';
import { assoc, dissoc } from 'sprout-data';

const _createAction = (description, ...args) => {
  return createAction('CUSTOMER_STORE_CREDIT_FORM_' + description, ...args);
};

export const changeScFormData = _createAction('CHANGE', (name, value) => [name, value]);
export const changeScAmount = _createAction('CHANGE_AMOUNT');
export const changeScReason = _createAction('CHANGE_REASON');
export const changeScType = _createAction('CHANGE_TYPE');

const submitStoreCredit = _createAction('SUBMIT');
const openStoreCreditList = _createAction('OPEN_LIST');
const failNewStoreCredit = _createAction('FAIL');
const resetForm = _createAction('RESET');

function changeGiftCardUrl(customerId, code) {
  return `/gift-cards/${code}/convert/${customerId}`;
}

function saveStoreCreditUrl(customerId) {
  return `/customers/${customerId}/payment-methods/store-credit`;
}

export function createStoreCredit(customerId) {
  return (dispatch, getState) => {
    dispatch(submitStoreCredit());
    const form = _.get(getState(), ['customers', 'newStoreCredit', 'form']);
    const payload = dissoc(form, 'code', 'id', 'availableAmount');

    Api.post(saveStoreCreditUrl(customerId), payload)
      .then(data => dispatch(openStoreCreditList(data)))
      .catch(err => dispatch(failNewStoreCredit(err)));
  };
}

export function convertGiftCard(customerId) {
  return (dispatch, getState) => {
    dispatch(submitStoreCredit());
    const form = _.get(getState(), ['customers', 'newStoreCredit', 'form']);
    const payload = dissoc(form, 'amount', 'currency', 'id', 'type', 'subTypeId', 'availableAmount');

    if (payload.code) {
      Api.post(changeGiftCardUrl(customerId, payload.code), payload)
        .then(data => dispatch(openStoreCreditList(data)))
        .catch(err => dispatch(failNewStoreCredit(err)));
    }
  };
}

export function changeGCCode(value) {
  return (dispatch) => {
    dispatch(changeScFormData('code', value));

    if (!_.isEmpty(value)) {
      Api.get(`/gift-cards/${value}`)
        .then(json => {
          dispatch(changeScFormData('availableAmount', json.currentBalance));
        })
        .catch(err => dispatch(changeScFormData('availableAmount', 0.0)));
    }
  };
}

export function resetScForm() {
  return (dispatch) => {
    dispatch(resetForm());
  };
}

const initialState = {
  form: {
    id: null,
    amount: null,
    currency: null,
    type: null,
    subTypeId: null,
    reasonId: null,
    code: null,
    availableAmount: 0
  },
  isFetching: false
};

const reducer = createReducer({
  [changeScFormData]: (state, [name, value]) => {
    return assoc(state, ['form', name], value);
  },
  [changeScAmount]: (state, newAmount) => {
    return assoc(state, ['form', 'amount'], newAmount);
  },
  [changeScReason]: (state, newReason) => {
    return assoc(state,
      ['form', 'reasonId'], newReason,
      ['form', 'subReasonId'], null
    );
  },
  [changeScType]: (state, newType) => {
    return assoc(state,
      ['form', 'type'], newType
    );
  },
  [submitStoreCredit]: state => {
    return assoc(state, 'isFetching', true);
  },
  [openStoreCreditList]: (state, payload) => {
    return assoc(state,
      'isFetching', false,
      ['form', 'id'], payload.id
    );
  },
  [resetForm]: (state) => {
    return initialState;
  },
  [failNewStoreCredit]: (state, err) => {
    console.error(err);

    return {
      ...state,
      isFetching: false
    };
  }
}, initialState);

export default reducer;
