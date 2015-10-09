'use strict';

const path = require('path');
const assert = require('assert');
const sinon = require('sinon');
const Immutable = require('immutable');

describe('GiftCardTransactions Actions', function() {
  const Api = require(path.resolve('src/lib/api'));
  const AshesDispatcher = require(path.resolve('src/lib/dispatcher')).default;
  const giftCardTransactionActions = require(path.resolve('src/actions/gift-card-transactions'));
  const giftCardTransactionConstants = require(path.resolve('src/constants/gift-card-transactions'));

  beforeEach(function() {
    this.dispatchSpy = sinon.spy(AshesDispatcher, 'handleViewAction');
  });

  afterEach(function() {
    this.dispatchSpy.restore();
  });

  context('updateTransactions', function() {
    it('should dispatch', function () {
      giftCardTransactionActions.updateTransactions(1, []);
      assert(this.dispatchSpy.calledWith({
        actionType: giftCardTransactionConstants.UPDATE_TRANSACTIONS,
        giftCard: 1,
        transactions: []
      }));
    });
  });

  context('failedTransactions', function() {
    it('should dispatch', function(){
      giftCardTransactionActions.failedTransactions('Error');
      assert(this.dispatchSpy.calledWith({
        actionType: giftCardTransactionConstants.FAILED_TRANSACTIONS,
        errorMessage: 'Error'
      }));
    });
  });

  context('fetchTransactions', function() {
    it('should dispatch and call updateTransactions on success', function(done){
      const response = 1;
      let spy = this.dispatchSpy;
      let stub = sinon.stub(Api, 'get').returns(Promise.resolve(response));

      giftCardTransactionActions.fetchTransactions(response).then(function(cards) {
        assert(spy.calledWith({
          actionType: giftCardTransactionConstants.UPDATE_TRANSACTIONS,
          giftCard: response,
          transactions: Immutable.List([response])
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

      giftCardTransactionActions.fetchTransactions().then(function(cards) {
        assert(spy.calledWith({
          actionType: giftCardTransactionConstants.FAILED_TRANSACTIONS,
          errorMessage: response
        }));
        done();
      }).catch(function(err) {
        done(err);
      });

      stub.restore();
    });
  });
});
