'use strict';

const path = require('path');
const assert = require('assert');
const sinon = require('sinon');
const Immutable = require('immutable');

describe('Customers Actions', function() {
  const Api = require(path.resolve('src/lib/api'));
  const AshesDispatcher = require(path.resolve('src/lib/dispatcher')).default;
  const customerActions = require(path.resolve('src/actions/customers'));
  const customerConstants = require(path.resolve('src/constants/customers'));

  beforeEach(function() {
    this.dispatchSpy = sinon.spy(AshesDispatcher, 'handleAction');
  });

  afterEach(function() {
    this.dispatchSpy.restore();
  });

  context('updateCustomers', function () {
    it('should dispatch', function () {
      customerActions.updateCustomers([]);
      assert(this.dispatchSpy.calledWith({
        actionType: customerConstants.UPDATE_CUSTOMERS,
        customers: []
      }));
    });
  });

  context('fetchCustomers', function () {
    it('should dispatch and call updateCustomers on success', function(done){
      const response = [1];
      let spy = this.dispatchSpy;
      let stub = sinon.stub(Api, 'get').returns(Promise.resolve(response));

      customerActions.fetchCustomers().then(function(cards) {
        assert(spy.calledWith({
          actionType: customerConstants.UPDATE_CUSTOMERS,
          customers: Immutable.List(response)
        }));
        done();
      }).catch(function(err) {
        done(err);
      });

      stub.restore();
    });

    it('should dispatch and call failedCustomers on failure', function(done){
      const response = 'Error';
      let spy = this.dispatchSpy;
      let stub = sinon.stub(Api, 'get').returns(Promise.reject(response));

      customerActions.fetchCustomers().then(function(err) {
        assert(spy.calledWith({
          actionType: customerConstants.FAILED_CUSTOMERS,
          errorMessage: response
        }));
        done();
      }).catch(function(err) {
        done(err);
      });

      stub.restore();
    });
  });

  context('failedCustomers', function() {
    it('should dispatch', function(){
      customerActions.failedCustomers('Error');
      assert(this.dispatchSpy.calledWith({
        actionType: customerConstants.FAILED_CUSTOMERS,
        errorMessage: 'Error'
      }));
    });
  });

  context('insertCustomer', function() {
    it('should dispatch', function () {
      customerActions.insertCustomer({});
      assert(this.dispatchSpy.calledWith({
        actionType: customerConstants.INSERT_CUSTOMERS,
        customer: {}
      }));
    });
  });

  context('createCustomer', function () {
    it('should dispatch and call insertCustomer on success', function(done){
      const response = 1;
      let spy = this.dispatchSpy;
      let stub = sinon.stub(Api, 'submitForm').returns(Promise.resolve(response));

      customerActions.createCustomer({}).then(function(customers) {
        assert(spy.calledWith({
          actionType: customerConstants.INSERT_CUSTOMERS,
          customer: response
        }));
        done();
      }).catch(function(err) {
        done(err);
      });

      stub.restore();
    });

  });
});
