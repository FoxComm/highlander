'use strict';

const path = require('path');
const assert = require('assert');
const sinon = require('sinon');

describe('GiftCard Store', function() {
  const alt = require(path.resolve('src/alt'));
  const Api = require(path.resolve('src/lib/api'));
  const giftCardStore = require(path.resolve('src/stores/gift-cards'));
  const giftCardActions = require(path.resolve('src/actions/gift-cards'));

  it('listens for UPDATE_GIFT_CARDS', function () {
    alt.dispatcher.dispatch({
      action: giftCardActions.UPDATE_GIFT_CARDS,
      data: [1]
    });

    assert(giftCardStore.getState().giftCards.size === 1);
  });

  it('updates giftCards on FETCH_GIFT_CARDS', function () {
    let stub = sinon.stub(Api, 'get').returns(Promise.resolve([1]));
    alt.dispatcher.dispatch({
      action: giftCardActions.FETCH_GIFT_CARDS
    });

    assert(giftCardStore.getState().giftCards.size === 1);
    stub.restore();
  });

  it('listens for GIFT_CARDS_FAILED', function() {
    let spy = sinon.spy(console, 'error');
    alt.dispatcher.dispatch({
      action: giftCardActions.GIFT_CARDS_FAILED
    });

    assert(spy.calledOnce);
    spy.restore();
  });
});
