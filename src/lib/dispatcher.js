'use strict';

import _ from 'lodash';
import { camelize } from 'fleck';
import { EventEmitter } from 'events';

const emitter = new EventEmitter();

function eventBinding(event, bind, ctx) {
  let eventName = camelize(event);
  let pascal = camelize(event, true);

  if (ctx && ctx[`on${pascal}`]) {
    if (bind) {
      ctx[`on${pascal}`] = _.bind(ctx[`on${pascal}`], ctx);
    }
    emitter[bind ? 'addListener' : 'removeListener'](eventName, ctx[`on${pascal}`]);
  }
}

export function dispatch(event, ...args) {
  emitter.emit(event, ...args);
}

export function listenTo(event, ctx) {
  eventBinding(event, true, ctx);
}

export function stopListeningTo(event, ctx) {
  eventBinding(event, false, ctx);
}
