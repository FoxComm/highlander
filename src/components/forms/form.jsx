
'use strict';

import _ from 'lodash';
import React, { PropTypes } from 'react';
import {EventEmitter} from 'events';

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

  onSubmit(event) {
    event.preventDefault();

    let isValid = true;

    this._emit('submit', (isFieldValid) => {
      if (!isFieldValid) isValid = false;
    });

    if (isValid && this.props.onSubmit) {
      this.props.onSubmit(event);
    }
  }

  render() {
    let props = {...this.props, onSubmit: this.onSubmit.bind(this)};

    return (
      <form {...props}>
        {this.props.children}
      </form>
    );
  }
}
