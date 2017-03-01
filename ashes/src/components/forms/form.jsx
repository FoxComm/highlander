

import React, { PropTypes, Component } from 'react';
import ReactDOM from 'react-dom';
import { EventEmitter } from 'events';
import { autobind } from 'core-decorators';

export default class Form extends Component {

  static propTypes = {
    onSubmit: PropTypes.func,
    children: PropTypes.node
  };

  static childContextTypes = {
    formDispatcher: PropTypes.object
  };

  getChildContext() {
    if (!this._context) {
      const emitter = new EventEmitter();

      emitter.setMaxListeners(20);

      this._context = {
        formDispatcher: emitter
      };
    }

    return this._context;
  }

  _emit(type, ...args) {
    this.getChildContext().formDispatcher.emit(type, ...args);
  }

  checkValidity() {
    let isValid = true;
    this._emit('submit', (isFieldValid) => {
      if (!isFieldValid) isValid = false;
    });

    return isValid;
  }

  @autobind
  handleSubmit(event) {
    event.preventDefault();

    const props = this.props;

    let isValid = true;
    this._emit('submit', (isFieldValid) => {
      if (!isFieldValid) isValid = false;
    });

    const setErrors = errors => {
      this._emit('errors', errors || {});
    };

    if (isValid && props.onSubmit) {
      const willResolved = props.onSubmit(event) || Promise.resolve(null);
      willResolved.then(() => setErrors(null), setErrors);
    }
  }

  @autobind
  handleKeyPress(event) {
    if (event.keyCode === 13 /*enter*/) {
      event.preventDefault();
      const formReactDOM = ReactDOM.findDOMNode(this.refs.form);
      formReactDOM.dispatchEvent(new Event('submit'));
    }
  }

  render() {
    let props = {...this.props, onSubmit: this.handleSubmit};

    return (
      <form {...props} onKeyDown={this.handleKeyPress} ref="form">
        {this.props.children}
      </form>
    );
  }
}
