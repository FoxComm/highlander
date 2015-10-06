'use strict';

const path = require('path');
const assert = require('assert');
const sinon = require('sinon');

describe('GiftCard Actions', function() {
  const alt = require(path.resolve('src/alt'));
  const Api = require(path.resolve('src/lib/api'));
  const giftCardActions = require(path.resolve('src/actions/gift-cards'));

  beforeEach(function() {
    this.dispatchSpy = sinon.spy(alt.dispatcher, 'dispatch');
  });

  afterEach(function() {
    alt.dispatcher.dispatch.restore();
  });

  it('should dispatch on updateGiftCards', function () {
    giftCardActions.updateGiftCards([]);

    assert(this.dispatchSpy.calledOnce);
  });

  it('should dispatch on fetchGiftCards', function(){
    sinon.stub(Api, 'get').returns(Promise.resolve([1]));
    giftCardActions.fetchGiftCards();
    assert(this.dispatchSpy.calledOnce);
  });

  it('should dispatch on createGiftCard', function(){
    sinon.stub(Api, 'submitForm').returns(Promise.resolve([1]));
    giftCardActions.createGiftCard();
    assert(this.dispatchSpy.calledOnce);
  });

  it('should dispatch on giftCardsFailed', function(){
    giftCardActions.giftCardsFailed("Error");
    let args = this.dispatchSpy.args[0];
    let firstArg = args[0];
    assert(firstArg.data === "Error");
  });
});
