'use strict';

import fleck from 'fleck';
import { EventEmitter } from 'events';

const emitter = new EventEmitter();

function eventBinding(event, method, ctx) {
  let
    eventName = fleck.camelize(event),
    pascal    = fleck.camelize(event, true);

  if (ctx && ctx[`on${pascal}`]) {
    emitter[method](eventName, ctx[`on${pascal}`]);
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
