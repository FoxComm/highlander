'use strict';

import { camelize } from 'fleck';
import { EventEmitter } from 'events';

const emitter = new EventEmitter();

function eventBinding(event, method, ctx, callback) {
  let eventName = camelize(event);
  let pascal = camelize(event, true);
  let func = callback || ctx[`on${pascal}`];

  if (ctx && func) {
    if (method === 'addListener') {
      func = func.bind(ctx);
    }
    emitter[method](eventName, func);
  }
}

export function dispatch(event, data) {
  emitter.emit(event, data);
}

export function listenTo(event, ctx, callback) {
  eventBinding(event, 'addListener', ctx, callback);
}

export function stopListeningTo(event, ctx) {
  eventBinding(event, 'removeListener', ctx);
}
