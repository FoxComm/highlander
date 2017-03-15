
import React, { PropTypes, Component } from 'react';
import { autobind } from 'core-decorators';
import { EventEmitter } from 'events';
import type { HTMLElement } from 'types';

type FormProps = {
  onSubmit?: Function;
  validate?: (data: Object) => Promise|void;
  children: HTMLElement;
}

export default class Form extends Component {
  props: FormProps;

  static propTypes = {
    onSubmit: PropTypes.func,
    children: PropTypes.node,
  };

  static childContextTypes = {
    formDispatcher: PropTypes.object,
  };

  getChildContext() {
    return this._context || (this._context = {
      formDispatcher: new EventEmitter(),
    });
  }

  _emit(type, ...args) {
    this.getChildContext().formDispatcher.emit(type, ...args);
  }

  @autobind
  onSubmit(event) {
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
    const props = {...this.props, onSubmit: this.onSubmit};

    return (
      <form {...props}>
        {this.props.children}
      </form>
    );
  }
}
