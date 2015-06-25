'use strict';

import { camelize } from 'fleck';
import { EventEmitter } from 'events';

const emitter = new EventEmitter();

function eventBinding(event, method, ctx) {
  let
    eventName = camelize(event),
    pascal    = camelize(event, true);

  if (ctx && ctx[`on${pascal}`]) {
    if (method === 'addListener') {
      ctx[`on${pascal}`] = ctx[`on${pascal}`].bind(ctx);
    }
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
