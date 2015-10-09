'use strict';

const path = require('path');
const assert = require('assert');
const sinon = require('sinon');
const Immutable = require('immutable');

describe('dispatcher', function() {
  const dispatcher = require(path.resolve('src/lib/dispatcher'));
  const AshesDispatcher = require(path.resolve('src/lib/dispatcher')).default;

  it('should stop listening events', function() {
    let ctx = {
      onEvent: function(arg) {
      }
    };
    let spy = sinon.spy(ctx, 'onEvent');

    dispatcher.listenTo('event', ctx);

    dispatcher.dispatch('event', 42);

    assert(spy.calledOnce);
    assert(spy.calledWith(42));

    dispatcher.stopListeningTo('event', ctx);
    dispatcher.dispatch('event', 42);
    assert(spy.calledOnce, 'should stop listening events after stopListeningTo call');
  });

  context('queue', function() {
    it('should add to queue when already running', function() {
      AshesDispatcher.isDispatching = true;

      AshesDispatcher.dispatch({});

      assert(AshesDispatcher.queue.size === 1);

      AshesDispatcher.resetQueue();
    });

    it('should process callback when can run', function() {
      let payload = {
        action: {
          actionType: 'HELLO'
        }
      };

      AshesDispatcher.isDispatching = true;

      AshesDispatcher.dispatch(payload);

      assert(AshesDispatcher.queue.size === 1);
      AshesDispatcher.resetQueue();

      AshesDispatcher.dispatch(payload);

      assert(AshesDispatcher.queue.size === 0);
      AshesDispatcher.resetQueue();
    });
  });

  context('callbacks', function() {
    it('should add callback to list', function() {
      let callback = () => {};
      let count = AshesDispatcher.callbacks.size;

      AshesDispatcher.register(callback);

      assert(AshesDispatcher.callbacks.size > count);

      AshesDispatcher.callbacks.delete(AshesDispatcher.callbacks.size - 1);
    });
  });

  context('waitFor', function() {
    it('should complete callback once waitFor done', function(done) {
      const GiftCardStore = require(path.resolve('src/stores/gift-cards'));

      let payload = {
        action: {
          actionType: 'HELLO'
        }
      };
      let callback = Promise.resolve().then(() => {
        assert(AshesDispatcher.isDispatching === false);
        done();
      }).catch((err) => {
        done(err);
      });

      AshesDispatcher.waitFor([GiftCardStore.dispatcherIndex], callback);

      AshesDispatcher.dispatch(payload);
    });
  });
});
