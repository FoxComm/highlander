'use strict';

const path = require('path');
const assert = require('assert');
const sinon = require('sinon');
const Immutable = require('immutable');

describe('GiftCard Actions', function() {
  const Api = require(path.resolve('src/lib/api'));
  const AshesDispatcher = require(path.resolve('src/lib/dispatcher')).default;
  const giftCardActions = require(path.resolve('src/actions/gift-cards'));
  const giftCardConstants = require(path.resolve('src/constants/gift-card'));

  beforeEach(function() {
    this.dispatchSpy = sinon.spy(AshesDispatcher, 'handleViewAction');
  });

  afterEach(function() {
    this.dispatchSpy.restore();
  });

  context('updateGiftCards', function() {
    it('should dispatch', function () {
      giftCardActions.updateGiftCards([]);
      assert(this.dispatchSpy.calledWith({
        actionType: giftCardConstants.UPDATE_GIFT_CARDS,
        giftCards: []
      }));
    });
  });

  context('fetchGiftCards', function() {
    it('should dispatch and call updateGiftCards on success', function(done){
      const response = [1];
      let spy = this.dispatchSpy;
      let stub = sinon.stub(Api, 'get').returns(Promise.resolve(response));

      giftCardActions.fetchGiftCards().then(function(cards) {
        assert(spy.calledWith({
          actionType: giftCardConstants.UPDATE_GIFT_CARDS,
          giftCards: Immutable.List(response)
        }));
        done();
      }).catch(function(err) {
        done(err);
      });

      stub.restore();
    });

    it('should dispatch and call giftCardsFailed on failure', function(done){
      const response = 'Error';
      let spy = this.dispatchSpy;
      let stub = sinon.stub(Api, 'get').returns(Promise.reject(response));
      
      giftCardActions.fetchGiftCards().then(function(err) {
        assert(spy.calledWith({
          actionType: giftCardConstants.GIFT_CARD_FAILED,
          errorMessage: response
        }));
        done();
      }).catch(function(err) {
        done(err);
      });

      stub.restore();
    });
  });

  context('createGiftCard', function() {
    it('should dispatch and call updateGiftCards on success', function(done){
      const response = 1;
      let spy = this.dispatchSpy;
      let stub = sinon.stub(Api, 'submitForm').returns(Promise.resolve(response));

      giftCardActions.createGiftCard().then(function(cards) {
        assert(spy.calledWith({
          actionType: giftCardConstants.UPDATE_GIFT_CARDS,
          giftCards: Immutable.List([response])
        }));
        done();
      }).catch(function(err) {
        done(err);
      });

      stub.restore();
    });

    it('should dispatch and call giftCardsFailed on failure', function(done){
      const response = 'Error';
      let spy = this.dispatchSpy;
      let stub = sinon.stub(Api, 'submitForm').returns(Promise.reject(response));

      giftCardActions.createGiftCard().then(function(err) {
        assert(spy.calledWith({
          actionType: giftCardConstants.GIFT_CARD_FAILED,
          errorMessage: response
        }));
        done();
      }).catch(function(err) {
        done(err);
      });

      stub.restore();
    });
  });

  context('giftCardsFailed', function() {
    it('should dispatch', function(){
      giftCardActions.giftCardsFailed('Error');
      assert(this.dispatchSpy.calledWith({
        actionType: giftCardConstants.GIFT_CARD_FAILED,
        errorMessage: 'Error'
      }));
    });
  });
});
