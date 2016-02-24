/* @flow */

import React, { Component, Element } from 'react';
import cssModules from 'react-css-modules';
import styles from './auth.css';
import { autobind } from 'core-decorators';

import { TextInput } from '../../common/inputs';
import { Form, FormField } from '../../forms';
import Button from '../../common/buttons';
import { Link } from 'react-router';


type RestoreState = {
  email: string,
  sent: boolean
};

/* ::`*/
@cssModules(styles)
/* ::`*/
export default class RestorePassword extends Component {

  state: RestoreState = {
    email: '',
    sent: false,
  };

  @autobind
  onChangeEmail({target}: SEvent<HTMLInputElement>) {
    this.setState({
      email: target.value,
    });
  }

  @autobind
  handleSubmit() {
    this.setState({
      sent: true,
    });
  }

  get topMessage(): Element {
    const { sent, email } = this.state;

    if (sent) {
      return (
        <div styleName="top-message-success">
          An email was successfully sent to <strong>{email}</strong> with reset instructions!
        </div>
      );
    }

    return (
      <div styleName="top-message">
        No worries! We’ll email you instructions on how to reset your password.
      </div>
    );
  }

  get emailField(): ?Element {
    const { sent, email } = this.state;

    if (sent) return null;

    return (
      <FormField key="email" styleName="form-field">
        <TextInput placeholder="EMAIL" required value={email} type="email" onChange={this.onChangeEmail} />
      </FormField>
    );
  }

  render(): Element {
    return (
      <div>
        <div styleName="title">FORGOT PASSWORD</div>
        {this.topMessage}
        <Form onSubmit={this.handleSubmit}>
          {this.emailField}
          <Button styleName="primary-button">SUBMIT</Button>
        </Form>
        <div styleName="switch-stage">
          <Link to="/login">BACK TO LOG IN</Link>
        </div>
      </div>
    );
  }
}
