'use strict';

const path = require('path');
const assert = require('assert');
const Immutable = require('immutable');
const sinon = require('sinon');

describe('GiftCardTransaction Store', function() {
  const AshesDispatcher = require(path.resolve('src/lib/dispatcher')).default;
  const giftCardTransactionActions = require(path.resolve('src/actions/gift-card-transactions'));
  const giftCardTransactionStore = require(path.resolve('src/stores/gift-card-transactions'));
  const giftCardTransactionConstants = require(path.resolve('src/constants/gift-card-transactions'));

  it('listens for UPDATE_TRANSACTIONS', function () {
    giftCardTransactionStore.setState(Immutable.List([]));

    giftCardTransactionActions.updateTransactions(1, Immutable.List([1]));

    assert(giftCardTransactionStore.getState().size === 1);
  });
});
