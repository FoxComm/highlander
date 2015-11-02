'use strict';

const path = require('path');
const assert = require('assert');
const sinon = require('sinon');
const Immutable = require('immutable');

// describe('Order Actions', function() {
//   const Api = require(path.resolve('src/lib/api'));
//   const AshesDispatcher = require(path.resolve('src/lib/dispatcher')).default;
//   const orderActions = require(path.resolve('src/actions/orders'));
//   const orderConstants = require(path.resolve('src/constants/orders'));

//   beforeEach(function() {
//     this.dispatchSpy = sinon.spy(AshesDispatcher, 'handleAction');
//   });

//   afterEach(function() {
//     this.dispatchSpy.restore();
//   });

//   context('updateOrders', function() {
//     it('should dispatch', function() {
//       orderActions.updateOrders([]);
//       assert(this.dispatchSpy.calledWith({
//         actionType: orderConstants.UPDATE_ORDERS,
//         orders: []
//       }));
//     });
//   });

//   context('failedOrders', function() {
//     it('should dispatch', function() {
//       orderActions.failedOrders('Error');
//       assert(this.dispatchSpy.calledWith({
//         actionType: orderConstants.FAILED_ORDERS,
//         errorMessage: 'Error'
//       }));
//     });
//   });

//   context('insertOrder', function() {
//     it('should dispatch', function() {
//       orderActions.insertOrder({});
//       assert(this.dispatchSpy.calledWith({
//         actionType: orderConstants.INSERT_ORDER,
//         order: {}
//       }));
//     });
//   });

//   context('updateOrderStatus', function() {
//     it('should dispatch and call insertOrder on success', function(done){
//       const response = {};
//       let spy = this.dispatchSpy;
//       let stub = sinon.stub(Api, 'patch').returns(Promise.resolve(response));

//       orderActions.updateOrderStatus('1', '2').then(function(cards) {
//         assert(spy.calledWith({
//           actionType: orderConstants.INSERT_ORDER,
//           order: response
//         }));
//         done();
//       }).catch(function(err) {
//         done(err);
//       });

//       stub.restore();
//     });

//     it('should dispatch and call failedOrders on failure', function(done){
//       const response = 'Error';
//       let spy = this.dispatchSpy;
//       let stub = sinon.stub(Api, 'patch').returns(Promise.reject(response));

//       orderActions.updateOrderStatus('1', '2').then(function(err) {
//         assert(spy.calledWith({
//           actionType: orderConstants.FAILED_ORDERS,
//           errorMessage: response
//         }));
//         done();
//       }).catch(function(err) {
//         done(err);
//       });

//       stub.restore();
//     });
//   });

//   context('fetchOrders', function() {
//     it('should dispatch and call updateOrders on success', function(done){
//       const response = [1];
//       let spy = this.dispatchSpy;
//       let stub = sinon.stub(Api, 'get').returns(Promise.resolve(response));

//       orderActions.fetchOrders().then(function(cards) {
//         assert(spy.calledWith({
//           actionType: orderConstants.UPDATE_ORDERS,
//           orders: Immutable.List(response)
//         }));
//         done();
//       }).catch(function(err) {
//         done(err);
//       });

//       stub.restore();
//     });

//     it('should dispatch and call failedOrders on failure', function(done){
//       const response = 'Error';
//       let spy = this.dispatchSpy;
//       let stub = sinon.stub(Api, 'get').returns(Promise.reject(response));

//       orderActions.fetchOrders().then(function(err) {
//         assert(spy.calledWith({
//           actionType: orderConstants.FAILED_ORDERS,
//           errorMessage: response
//         }));
//         done();
//       }).catch(function(err) {
//         done(err);
//       });

//       stub.restore();
//     });
//   });

//   context('fetchOrder', function() {
//     it('should dispatch and call insertOrder on success', function(done){
//       const response = {};
//       let spy = this.dispatchSpy;
//       let stub = sinon.stub(Api, 'get').returns(Promise.resolve(response));

//       orderActions.fetchOrder('1').then(function(cards) {
//         assert(spy.calledWith({
//           actionType: orderConstants.INSERT_ORDER,
//           order: response
//         }));
//         done();
//       }).catch(function(err) {
//         done(err);
//       });

//       stub.restore();
//     });

//     it('should dispatch and call failedOrders on failure', function(done){
//       const response = 'Error';
//       let spy = this.dispatchSpy;
//       let stub = sinon.stub(Api, 'get').returns(Promise.reject(response));

//       orderActions.fetchOrder('1').then(function(err) {
//         assert(spy.calledWith({
//           actionType: orderConstants.FAILED_ORDERS,
//           errorMessage: response
//         }));
//         done();
//       }).catch(function(err) {
//         done(err);
//       });

//       stub.restore();
//     });
//   });
// });
