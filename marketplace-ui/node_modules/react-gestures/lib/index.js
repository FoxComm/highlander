'use strict';

var _inherits = require('babel-runtime/helpers/inherits')['default'];

var _get = require('babel-runtime/helpers/get')['default'];

var _createClass = require('babel-runtime/helpers/create-class')['default'];

var _classCallCheck = require('babel-runtime/helpers/class-call-check')['default'];

var _Object$defineProperty = require('babel-runtime/core-js/object/define-property')['default'];

var _interopRequireDefault = require('babel-runtime/helpers/interop-require-default')['default'];

_Object$defineProperty(exports, '__esModule', {
  value: true
});

var _react = require('react');

var _react2 = _interopRequireDefault(_react);

var Gestures = (function (_React$Component) {
  function Gestures(props) {
    var _this = this;

    _classCallCheck(this, Gestures);

    _get(Object.getPrototypeOf(Gestures.prototype), 'constructor', this).call(this, props);

    this._handleTouchStart = function (e) {
      _this._emitEvent('onTouchStart', e);

      _this.setState({
        start: Date.now(),
        x: e.touches[0].clientX,
        y: e.touches[0].clientY,
        swiping: false });
    };

    this._handleTouchMove = function (e) {
      var ge = _this._getGestureDetails(e);
      _this._emitEvent('onTouchMove', ge);

      if (ge.gesture.absX > _this.props.swipeThreshold && ge.gesture.absY > _this.props.swipeThreshold) {
        _this._handleSwipeGesture(ge);
        return;
      }
    };

    this._handleTouchCancel = function (e) {
      _this._emitEvent('onTouchCancel', e);
      _this._resetState();
    };

    this._handleTouchEnd = function (e) {
      var ge = _this._getGestureDetails(e);
      _this._emitEvent('onTouchEnd', ge);

      if (_this.state.swiping) {
        _this._handleSwipeGesture(ge);
        return _this._resetState();
      }
      if (ge.gesture.duration > 0) {
        _this._handleTapGesture(ge);
      }
      _this._resetState();
    };

    this.state = {
      x: null,
      y: null,
      swiping: false,
      start: 0 };
  }

  _inherits(Gestures, _React$Component);

  _createClass(Gestures, [{
    key: '_resetState',
    value: function _resetState() {
      this.setState({ x: null, y: null, swiping: false, start: 0 });
    }
  }, {
    key: '_emitEvent',
    value: function _emitEvent(name, e) {
      if (this.props[name]) {
        this.props[name](e);
      }
    }
  }, {
    key: '_getGestureDetails',
    value: function _getGestureDetails(e) {
      var _e$changedTouches$0 = e.changedTouches[0];
      var clientX = _e$changedTouches$0.clientX;
      var clientY = _e$changedTouches$0.clientY;

      var deltaX = this.state.x - clientX;
      var deltaY = this.state.y - clientY;
      var absX = Math.abs(deltaX);
      var absY = Math.abs(deltaY);
      var duration = Date.now() - this.state.start;
      var velocity = Math.sqrt(absX * absX + absY * absY) / duration;
      var done = e.type === 'touchend';
      e.gesture = { deltaX: deltaX, deltaY: deltaY, absX: absX, absY: absY, velocity: velocity, duration: duration, done: done };
      return e;
    }
  }, {
    key: '_handleTapGesture',
    value: function _handleTapGesture(ge) {
      ge.type = 'tap';
      this._emitEvent('onTap', ge);
    }
  }, {
    key: '_handleSwipeGesture',
    value: function _handleSwipeGesture(ge) {
      var _ge$gesture = ge.gesture;
      var deltaX = _ge$gesture.deltaX;
      var absX = _ge$gesture.absX;
      var deltaY = _ge$gesture.deltaY;
      var absY = _ge$gesture.absY;

      var direction = absX > absY ? deltaX < 0 ? 'Right' : 'Left' : deltaY < 0 ? 'Up' : 'Down';

      this.setState({ swiping: true });

      ge.gesture.isFlick = ge.gesture.velocity > this.props.flickThreshold;
      ge.type = 'swipe' + direction.toLowerCase();
      this._emitEvent('onSwipe' + direction, ge);
      ge.preventDefault();
    }
  }, {
    key: 'render',
    value: function render() {
      return _react2['default'].cloneElement(_react2['default'].Children.only(this.props.children), {
        onTouchStart: this._handleTouchStart,
        onTouchMove: this._handleTouchMove,
        onTouchCancel: this._handleTouchCancel,
        onTouchEnd: this._handleTouchEnd });
    }
  }], [{
    key: 'propTypes',
    value: {
      onSwipeUp: _react2['default'].PropTypes.func,
      onSwipeDown: _react2['default'].PropTypes.func,
      onSwipeLeft: _react2['default'].PropTypes.func,
      onSwipeRight: _react2['default'].PropTypes.func,
      flickThreshold: _react2['default'].PropTypes.number,
      swipeThreshold: _react2['default'].PropTypes.number },
    enumerable: true
  }, {
    key: 'defaultProps',
    value: {
      flickThreshold: 0.6,
      swipeThreshold: 10 },
    enumerable: true
  }]);

  return Gestures;
})(_react2['default'].Component);

exports['default'] = Gestures;
module.exports = exports['default'];