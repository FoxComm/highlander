'use strict';

const path = require('path');
const assert = require('assert');
const sinon = require('sinon');

describe('dispatcher', function() {
  const dispatcher = require(path.resolve('src/themes/admin/lib/dispatcher'));

  it('should stop listening events', function() {
    let spy = sinon.spy();
    let ctx = {
      onEvent: function(arg) {
        spy(arg);
      }
    };

    dispatcher.listenTo('event', ctx);

    dispatcher.dispatch('event', 42);

    assert(spy.calledOnce);
    assert(spy.calledWith(42));

    dispatcher.stopListeningTo('event', ctx);
    dispatcher.dispatch('event', 42);
    assert(spy.calledOnce, 'should stop listening events after stopListeningTo call');
  });
});