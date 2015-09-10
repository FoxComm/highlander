'use strict';

import Api from './api';
import { dispatch, listenTo, stopListeningTo } from './dispatcher';
import { inflect } from 'fleck';

export default class EventedStore {
  get storeName() { return this.constructor.name; }
  get eventSuffix() { return inflect(this.storeName, 'underscore', 'dasherize'); }

  dispatch(event, ...args) {
    dispatch(`${event}${this.storeName}`, ...args);
  }

  listenToEvent(event, ctx) {
    listenTo(`${event}-${this.eventSuffix}`, ctx);
  }

  stopListeningToEvent(event, ctx) {
    stopListeningTo(`${event}-${this.eventSuffix}`, ctx);
  }

  apiError(err) {
    console.error(err);
    this.dispatch('error', err);
  }
}