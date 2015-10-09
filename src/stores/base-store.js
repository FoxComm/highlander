import { EventEmitter } from 'events';
import { Map } from 'immutable';
import AshesDispatcher from '../lib/dispatcher';

export default class BaseStore extends EventEmitter {
  constructor() {
    super();
    this.state = Map({});
    this.changeEvent = 'change';
    this.listeners = Map({});

    this.dispatcherIndex = AshesDispatcher.register(this.dispatcherCallback.bind(this));
  }

  get dispatcherIndex() {
    return this._dispatcherIndex;
  }

  set dispatcherIndex(value) {
    this._dispatcherIndex = value;
  }

  getState() {
    return this.state;
  }

  setState(state) {
    this.state = state;
    this.emitChange();
  }

  emitChange() {
    this.emit(this.changeEvent);
  }

  listen(callback) {
    this.on(this.changeEvent, callback);
  }

  unlisten(callback) {
    this.removeListener(this.changeEvent, callback);
  }

  bindListener(actionType, callback) {
    callback = callback.bind(this);
    this.listeners = this.listeners.set(actionType, callback);
  }

  dispatcherCallback(payload) {
    const action = payload.action;
    const actionType = action.actionType;

    if (this.listeners.has(actionType)) {
      this.listeners.get(actionType)(action);
    }
  }

  insertIntoList(list, newItem, field='id') {
    let existingIndex = list.findIndex(item => item[field] === newItem[field]);
    if (existingIndex === -1) existingIndex = this.state.size;
    return list.set(existingIndex, newItem);
  }
}
