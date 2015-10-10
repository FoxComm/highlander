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

  beforeEach(function() {
    giftCardStore.setState(Immutable.List([]));
  });

  it('listens for UPDATE_GIFT_CARDS', function () {
    giftCardActions.updateGiftCards(Immutable.List([1]));

    assert(giftCardStore.getState().size === 1);
  });

  it('listens for INSERT_GIFT_CARD', function() {
    giftCardActions.insertGiftCard({giftCard: {'code': 1}});

    assert(giftCardStore.getState().size === 1);
  });
});
