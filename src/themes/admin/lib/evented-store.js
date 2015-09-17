'use strict';

import _ from 'lodash';
import Api from './api';
import { dispatch, listenTo, stopListeningTo } from './dispatcher';
import { inflect } from 'fleck';

export default class EventedStore {
  get storeName() { return this.constructor.name; }
  get eventSuffix() { return inflect(this.storeName, 'underscore', 'dasherize'); }

  _findModel(models, id) {
    return _.find(models, 'id', id);
  }

  _findWhere(models, ...args) {
    return _.findWhere(models, ...args);
  }

  process(model) {
    return model;
  }

  _sort(models, field, order) {
    return models.sort((a, b) => {
      return (1 - 2 * order) * (a[field] < b[field] ? 1 : a[field] > b[field] ? -1 : 0);
    });
  }

  _upsert(models, model) {
    let exists = this._findModel(models, model.id);
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

  create(data, ...fetchArgs) {
    return Api.post(this.uri(...fetchArgs), data)
      .then((res) => {
        this.updateBehaviour(res, ...fetchArgs);
        return res;
      })
      .catch((err) => {
        throw this.apiError(err);
      });
  }

  apiError(err) {
    console.error(String(err));
    this.dispatch('error', err);
    return err;
  }
}