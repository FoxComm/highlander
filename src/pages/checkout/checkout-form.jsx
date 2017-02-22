/* @flow */

// libs
import React, { Component } from 'react';
import sanitizeAll from 'sanitizers';

// components
import { Form } from 'ui/forms';
import Button from 'ui/buttons';
import ErrorAlerts from '@foxcomm/wings/lib/ui/alerts/error-alerts';

// styles
import styles from './checkout-form.css';

type Props = {
  title?: string,
  error: ?Array<any>|Object|null,
  submit: Function,
  action?: ?Object,
  children?: any,
  buttonLabel?: ?string,
  inProgress?: boolean,
  sanitizeError?: (error: string) => string,
};

class CheckoutForm extends Component {
  props: Props;

  get actionLink() {
    if (this.props.action) {
      return (
        <span styleName="action-link" onClick={this.props.action.action}>
          {this.props.action.title}
        </span>
      );
    }
  }

  get buttonLabel(): string {
    return this.props.buttonLabel || 'Continue';
  }

  get header() {
    const { props } = this;

    if (props.title || props.action) {
      return (
        <div styleName="form-header" key="header">
          <legend styleName="legend">{props.title}</legend>
          {this.actionLink}
        </div>
      );
    }
  }

  render() {
    const { props } = this;
    const { sanitizeError = sanitizeAll } = props;

    return (
      <Form onSubmit={props.submit} styleName="root">
        {this.header}
        {props.children}
        <ErrorAlerts
          sanitizeError={sanitizeError}
          error={props.error}
        />
        <div styleName="button-wrap">
          <Button styleName="checkout-submit" type="submit" isLoading={props.inProgress}>{this.buttonLabel}</Button>
        </div>
      </Form>
    );
  }
}

export default CheckoutForm;
