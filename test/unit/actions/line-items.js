'use strict';

const path = require('path');
const assert = require('assert');
const sinon = require('sinon');
const Immutable = require('immutable');

describe('LineItem Actions', function() {
  const Api = require(path.resolve('src/lib/api'));
  const AshesDispatcher = require(path.resolve('src/lib/dispatcher')).default;
  const lineItemActions = require(path.resolve('src/actions/line-items'));
  const lineItemConstants = require(path.resolve('src/constants/line-items'));

  beforeEach(function() {
    this.dispatchSpy = sinon.spy(AshesDispatcher, 'handleAction');
  });

  afterEach(function() {
    this.dispatchSpy.restore();
  });

  context('orderLineItemSuccess', function() {
    it('should dispatch', function() {
      lineItemActions.orderLineItemSuccess({});
      assert(this.dispatchSpy.calledWith({
        actionType: lineItemConstants.ORDER_LINE_ITEM_SUCCESS,
        order: {}
      }));
    });
  });

  context('rmaLineItemSuccess', function() {
    it('should dispatch', function() {
      lineItemActions.rmaLineItemSuccess({});
      assert(this.dispatchSpy.calledWith({
        actionType: lineItemConstants.RETURN_LINE_ITEM_SUCCESS,
        rma: {}
      }));
    });
  });

  context('failedLineItems', function() {
    it('should dispatch', function() {
      lineItemActions.failedLineItems('Error');
      assert(this.dispatchSpy.calledWith({
        actionType: lineItemConstants.FAILED_LINE_ITEMS,
        errorMessage: 'Error'
      }));
    });
  });

  context('editLineItems', function () {
    it('should dispatch and call orderLineItemSuccess on order success', function(done){
      const {model, refNum, lineItems} = {model: 'order', refNum: '1', lineItems: []};
      const response = {};
      let spy = this.dispatchSpy;
      let stub = sinon.stub(Api, 'post').returns(Promise.resolve(response));

      lineItemActions.editLineItems(model, refNum, lineItems).then(function(cards) {
        assert(spy.calledWith({
          actionType: lineItemConstants.ORDER_LINE_ITEM_SUCCESS,
          order: response
        }));
        done();
      }).catch(function(err) {
        done(err);
      });

      stub.restore();
    });

    it('should dispatch and call rmaLineItemSuccess on order success', function(done){
      const {model, refNum, lineItems} = {model: 'return', refNum: '1', lineItems: []};
      const response = {};
      let spy = this.dispatchSpy;
      let stub = sinon.stub(Api, 'post').returns(Promise.resolve(response));

      lineItemActions.editLineItems(model, refNum, lineItems).then(function(cards) {
        assert(spy.calledWith({
          actionType: lineItemConstants.RETURN_LINE_ITEM_SUCCESS,
          rma: response
        }));
        done();
      }).catch(function(err) {
        done(err);
      });

      stub.restore();
    });

    it('should dispatch and call failedLineItems on failure', function(done){
      const response = 'Error';
      let spy = this.dispatchSpy;
      let stub = sinon.stub(Api, 'post').returns(Promise.reject(response));

      lineItemActions.editLineItems('order', '1', []).then(function(err) {
        assert(spy.calledWith({
          actionType: lineItemConstants.FAILED_LINE_ITEMS,
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
