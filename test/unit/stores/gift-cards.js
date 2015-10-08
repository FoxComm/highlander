'use strict';

const path = require('path');
const assert = require('assert');
const sinon = require('sinon');
const Immutable = require('immutable');

describe('GiftCard Store', function() {
  const AshesDispatcher = require(path.resolve('src/lib/dispatcher')).default;
  const giftCardActions = require(path.resolve('src/actions/gift-cards'));
  const giftCardStore = require(path.resolve('src/stores/gift-cards'));
  const giftCardConstants = require(path.resolve('src/constants/gift-card'));

  it('listens for UPDATE_GIFT_CARDS', function () {
    giftCardActions.updateGiftCards(Immutable.List([1]));

    assert(giftCardStore.getState().get('giftCards').size === 1);
  });
});
