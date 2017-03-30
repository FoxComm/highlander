/* @flow */

import React, { Component } from 'react';
import { autobind } from 'core-decorators';

import Button from 'ui/buttons';
import TextInput from 'ui/text-input/text-input';
import { FormField, Form } from 'ui/forms';

import styles from './auth.css';

type State = {
  email: string,
};

type Event = {
  target: {
    value: string,
  },
};

class Guest extends Component {
  state: State = {
    email: '',
  };

  @autobind
  onEmailChange({target}: Event) {
    this.setState({email: target.value});
  }

  @autobind
  onClick() {
    this.props.onGuestCheckout(this.state.email);
  }

  render() {
    return (
      <Form onSubmit={this.onClick}>
        <div styleName="title">Checkout as guest</div>
        <div styleName="inputs-body">
          <FormField key="email" styleName="form-field" required>
            <TextInput placeholder="Email" type="email" value={this.state.email} onChange={this.onEmailChange} />
          </FormField>
        </div>
        <Button
          styleName="primary-button"
          type="submit"
          children="Checkout"
        />
      </Form>
    );
  }
}

export default Guest;
