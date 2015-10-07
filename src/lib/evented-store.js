'use strict';

import _ from 'lodash';
import Api from './api';
import { EventEmitter } from 'events';
import { dispatch, listenTo, stopListeningTo } from './dispatcher';
import { inflect } from 'fleck';

export default class EventedStore extends EventEmitter {
  get storeName() { return this.constructor.name; }
  get eventSuffix() { return inflect(this.storeName, 'underscore', 'dasherize'); }

  /**
   * It should return a field in which an object can be compared
   * @param model
   * @returns {*}
   */
  identity(model) {
    return model.id;
  }

  _findModel(models, id) {
    return _.find(models, (model) => {
      return this.identity(model) === id;
    });
  }

  _findWhere(models, ...args) {
    return _.findWhere(models, ...args);
  }

  process(model) {
    return model;
  }

  _sort(models, field, order=1) {
    return models.sort((a, b) => {
      return (1 - 2 * order) * (a[field] < b[field] ? 1 : a[field] > b[field] ? -1 : 0);
    });
  }

  _upsert(models, model) {
    let exists = this._findModel(models, this.identity(model));
    if (exists) {
      let idx = models.indexOf(exists);
      if (idx !== -1) {
        models[idx] = this.process(model) || model;
      }
    } else {
      models.push(this.process(model) || model);
    }
  }

  _update(models, model) {
    if (_.isArray(model)) {
      model.forEach((item) => {
        this._upsert(models, item);
      });
    } else {
      this._upsert(models, model);
    }
  }

  dispatch(event, ...args) {
    this.emit(event, ...args);
    dispatch(`${event}${this.storeName}`, ...args);
  }

  listenToEvent(event, ctx) {
    listenTo(`${event}-${this.eventSuffix}`, ctx);
  }

  stopListeningToEvent(event, ctx) {
    stopListeningTo(`${event}-${this.eventSuffix}`, ctx);
  }

  updateBehaviour() {
    throw new Error('updateBehaviour not implemented');
  }

  fetch(...fetchArgs) {
    return Api.get(this.uri(...fetchArgs))
      .then((res) => {
        this.updateBehaviour(res, ...fetchArgs);
        return res;
      })
      .catch((err) => {
        throw this.apiError(err);
      });
  }

  patch(changes, ...fetchArgs) {
    return Api.patch(this.uri(...fetchArgs), changes)
      .then((res) => {
        this.updateBehaviour(res, ...fetchArgs);
        return res;
      })
      .catch((err) => {
        throw this.apiError(err);
      });
  }

  post(data, ...fetchArgs) {
    return Api.post(this.uri(...fetchArgs), data)
      .then((res) => {
        this.updateBehaviour(res, ...fetchArgs);
        return res;
      })
      .catch((err) => {
        throw this.apiError(err);
      });
  }

  create(data, ...fetchArgs) {
    this.post(data, ...fetchArgs);
  }

  delete(id) {
    return Api.delete(this.uri(id))
      .then((res) => {
        this.update(res);
      })
      .catch((err) => {
        this.apiError(err);
      });
  }

  apiError(err) {
    console.error(String(err));
    this.dispatch('error', err);
    return err;
  }
}
