/* @flow */

// libs
import React, { Component } from 'react';

// components
import { Form } from 'ui/forms';
import Button from 'ui/buttons';
import ErrorAlerts from '@foxcomm/wings/lib/ui/alerts/error-alerts';

// styles
import styles from './checkout-form.css';

type Props = {
  title: string,
  error: ?Array<any>|Object,
  submit: Function,
  action?: ?Object,
  children?: any,
  buttonLabel?: ?string,
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

  render() {
    return (
      <Form onSubmit={this.props.submit}>
        <div styleName="form-header">
          <legend styleName="legend">{this.props.title}</legend>
          {this.actionLink}
        </div>

        {this.props.children}

        <ErrorAlerts error={this.props.error} />
        <div styleName="button-wrap">
          <Button styleName="checkout-submit" type="submit">{this.buttonLabel}</Button>
        </div>
      </Form>
    );
  }
}

export default CheckoutForm;
