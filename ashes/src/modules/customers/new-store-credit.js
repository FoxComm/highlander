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
export const resetForm = _createAction('RESET');

const submitStoreCredit = _createAction('SUBMIT');
const openStoreCreditList = _createAction('OPEN_LIST');
const failNewStoreCredit = _createAction('FAIL');

function changeGiftCardUrl(customerId, code) {
  const uppercasedCode = _.toUpper(code);
  return `/gift-cards/${uppercasedCode}/convert/${customerId}`;
}

function saveStoreCreditUrl(customerId) {
  return `/customers/${customerId}/payment-methods/store-credit`;
}

export function createStoreCredit(customerId) {
  return (dispatch, getState) => {
    dispatch(submitStoreCredit());
    const form = _.get(getState(), ['customers', 'newStoreCredit', 'form']);
    const payload = dissoc(form, 'code', 'id', 'availableAmount');

    return Api.post(saveStoreCreditUrl(customerId), payload)
      .then(
        data => dispatch(openStoreCreditList(data)),
        err => dispatch(failNewStoreCredit(err))
      );
  };
}

export function convertGiftCard(customerId) {
  return (dispatch, getState) => {
    dispatch(submitStoreCredit());
    const form = _.get(getState(), ['customers', 'newStoreCredit', 'form']);
    const payload = dissoc(form, 'amount', 'currency', 'id', 'type', 'subTypeId', 'availableAmount');

    if (payload.code) {
      return Api.post(changeGiftCardUrl(customerId, payload.code), payload)
        .then(
          data => dispatch(openStoreCreditList(data)),
          err => dispatch(failNewStoreCredit(err))
        );
    }
  };
}

export function changeGCCode(value) {
  return (dispatch) => {
    dispatch(changeScFormData('code', value));

    if (!_.isEmpty(value)) {
      const uppercasedCode = _.toUpper(value);
      return Api.get(`/gift-cards/${uppercasedCode}`)
        .then(
          json => dispatch(changeScFormData('availableAmount', json.availableBalance)),
          err => dispatch(changeScFormData('availableAmount', 0.0))
        );
    }
  };
}

const amountToText = amount => (amount / 100).toFixed(2);
const textToAmount = value => value * 100;

const initialState = {
  form: {
    id: null,
    amount: null,
    amountText: amountToText(0),
    currency: 'USD',
    type: null,
    subTypeId: null,
    reasonId: null,
    code: null,
    availableAmount: 0
  },
  balances: [1000, 2500, 5000, 10000, 20000],
  isFetching: false,
  error: null,
};

const reducer = createReducer({
  [changeScFormData]: (state, [name, value]) => {
    if (name === 'amountText') {
      return assoc(state,
        ['form', 'amount'], textToAmount(value),
        ['form', 'amountText'], value
      );
    }
    if (name === 'amount') {
      return assoc(state,
        ['form', 'amountText'], amountToText(value),
        ['form', 'amount'], value
      );
    }
    return assoc(state,
      ['form', name], value,
      'error', null
    );
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
    return assoc(state,
      'isFetching', true,
      'error', null
    );
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
      error: _.get(err, 'response.body.errors', []),
      isFetching: false,
    };
  }
}, initialState);

export default reducer;
