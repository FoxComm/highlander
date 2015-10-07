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

  context('updateGiftCards', function() {
    it('should dispatch', function () {
      giftCardActions.updateGiftCards([]);

      assert(this.dispatchSpy.calledOnce);
    });
  });

  context('fetchGiftCards', function() {
    it('should dispatch and call updateGiftCards on success', function(done){
      let response = [1];
      let spy = sinon.spy(giftCardActions, 'updateGiftCards');
      let stub = sinon.stub(Api, 'get').returns(Promise.resolve(response));

      giftCardActions.fetchGiftCards().then(function(cards) {
        assert(spy.calledWith(response));
        spy.restore();
        done();
      }).catch(function(err) {
        done(err);
      });

      assert(this.dispatchSpy.calledOnce);
      stub.restore();
    });

    it('should dispatch and call giftCardsFailed on failure', function(done){
      let spy = sinon.spy(giftCardActions, 'giftCardsFailed');
      let stub = sinon.stub(Api, 'get').returns(Promise.reject("Error"));

      giftCardActions.fetchGiftCards().then(function(err) {
        assert(spy.calledOnce);
        spy.restore();
        done();
      }).catch(function(err) {
        done(err);
      });

      assert(this.dispatchSpy.calledOnce);
      stub.restore();
    });
  });

  context('createGiftCard', function() {
    it('should dispatch and call updateGiftCards on success', function(done){
      let response = [1];
      let spy = sinon.spy(giftCardActions, 'updateGiftCards');
      let stub = sinon.stub(Api, 'submitForm').returns(Promise.resolve(response));

      giftCardActions.createGiftCard().then(function(cards) {
        assert(spy.calledWith(response));
        spy.restore();
        done();
      }).catch(function(err) {
        done(err);
      });

      assert(this.dispatchSpy.calledOnce);
      stub.restore();
    });

    it('should dispatch and call giftCardsFailed on failure', function(done){
      let spy = sinon.spy(giftCardActions, 'giftCardsFailed');
      let stub = sinon.stub(Api, 'submitForm').returns(Promise.reject("Error"));

      giftCardActions.createGiftCard().then(function(err) {
        assert(spy.calledOnce);
        spy.restore();
        done();
      }).catch(function(err) {
        done(err);
      });

      assert(this.dispatchSpy.calledOnce);
      stub.restore();
    });
  });

  context('giftCardsFailed', function() {
    it('should dispatch', function(){
      giftCardActions.giftCardsFailed("Error");
      let args = this.dispatchSpy.args[0];
      let firstArg = args[0];
      assert(firstArg.data === "Error");
    });
  });
});
