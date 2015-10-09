'use strict';

import _ from 'lodash';
import { camelize } from 'fleck';
import { EventEmitter } from 'events';
import { List } from 'immutable';

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

export class Dispatcher {
  constructor() {
    this.callbacks = List([]);
    this.promises = List([]);
    this.resetQueue();
  }

  register(callback) {
    this.callbacks = this.callbacks.push(callback);
    return this.callbacks.size - 1;
  }

  resetQueue() {
    this.isDispatching = false;
    
    this.queue = List([]);
  }

  dispatch(payload) {
    let resolves = List([]);
    let rejects = List([]);

    if (this.isDispatching) {
      this.queue = this.queue.push(payload);
      return;
    }

    this.isDispatching = true;

    this.promises = this.callbacks.map((_, i) => {
      return new Promise((resolve, reject) => {
        resolves = resolves.set(i, resolve);
        rejects = rejects.set(i, reject);
      });
    });

    this.callbacks.forEach((callback, i) => {
      Promise.resolve(callback(payload)).then(() => {
        resolves.get(i)(payload);
      }, () => {
        rejects.get(i)(new Error('Dispatcher callback unsuccessful.'));
      });
    });

    this.promises = this.promises.clear();
    this.isDispatching = false;

    if (this.queue.size > 0) {
      let nextPayload = this.queue.first();
      this.queue = this.queue.rest();
      this.dispatch(nextPayload);
    }
  }

  waitFor(promiseIndexes, callback) {
    let selectedPromises = promiseIndexes.map((index) => {
      return this.promises.get(index);
    });
    return Promise.all(selectedPromises).then(callback);
  }
}

class AppDispatcher extends Dispatcher {
  handleViewAction(action, source='VIEW_ACTION') {
    this.dispatch({
      source: source,
      action: action
    });
  }
}

let AshesDispatcher = new AppDispatcher();
export default AshesDispatcher;
