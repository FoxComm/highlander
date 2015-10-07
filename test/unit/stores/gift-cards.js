'use strict';

const path = require('path');
const assert = require('assert');
const sinon = require('sinon');

describe('GiftCard Store', function() {
  const alt = require(path.resolve('src/alt'));
  const giftCardStore = require(path.resolve('src/stores/gift-cards'));
  const giftCardActions = require(path.resolve('src/actions/gift-cards'));

  it('listens for update gift card action', function () {
    alt.dispatcher.dispatch({
      action: giftCardActions.UPDATE_GIFT_CARDS,
      data: [1]
    });

    assert(giftCardStore.getState().giftCards.size === 1);
  });

  it('updates giftCards on fetchGiftCards', function () {
    alt.dispatcher.dispatch({
      action: giftCardActions.FETCH_GIFT_CARDS
    });
  });
});
