
import _ from 'lodash';
import React, { PropTypes } from 'react';
import {EventEmitter} from 'events';
import { autobind } from 'core-decorators';

export default class Form extends React.Component {

  static propTypes = {
    onSubmit: PropTypes.func,
    children: PropTypes.node
  };

  static childContextTypes = {
    formDispatcher: PropTypes.object
  };

  constructor(props, context) {
    super(props, context);
  }

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

    const isValid = this.checkValidity();

    if (isValid && this.props.onSubmit) {
      this.props.onSubmit(event);
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
