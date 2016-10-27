

import React, { PropTypes, Component } from 'react';
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
    return this._context || (this._context = {
      formDispatcher: new EventEmitter()
    });
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

  render() {
    let props = {...this.props, onSubmit: this.handleSubmit};

    return (
      <form {...props}>
        {this.props.children}
      </form>
    );
  }
}
