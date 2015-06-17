'use strict';

const
  fleck         = require('fleck'),
  EventEmitter  = require('events').EventEmitter;

const
  emitter = new EventEmitter();

export default {

  dispatch(event, data) {
    emitter.emit(event, data);
  },

  listenTo(event) {
    var pascal = fleck.upperCamelize(event);

    var mixin = {
      componentWillMount() {
        if (this[`on${pascal}`]) {
          emitter.on(event, this[`on${pascal}`]);
        }
      },

      componentWillUnmount() {
        if (this[`on${pascal}`]) {
          emitter.off(event, this[`on${pascal}`]);
        }
      }
    };

    mixin[`emit${pascal}`] = emitter.emit.bind(emitter, event);
    return mixin;
  }
};
