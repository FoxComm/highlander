'use strict';

import fleck from 'fleck';
import { EventEmitter } from 'events';

const emitter = new EventEmitter();

function eventName(event) {
  return fleck.upperCamelize(event);
}

function eventBinding(event, method, ctx) {
  let pascal = eventName(event);
  if (ctx && ctx[`on${pascal}`]) {
    let cb = ctx[`on${pascal}`].bind(ctx);
    emitter[method](event, cb);
  }
}

export function dispatch(event, data) {
  emitter.emit(event, data);
}

export function listenTo(event, ctx) {
  eventBinding(event, 'addListener', ctx);
}

export function stopListeningTo(event, ctx) {
  eventBinding(event, 'removeListener', ctx);
}
