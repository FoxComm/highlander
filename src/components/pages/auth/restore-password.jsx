/* @flow */

import React, { Component, PropTypes } from 'react';
import cssModules from 'react-css-modules';
import styles from './auth.css';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { routeActions } from 'react-router-redux';

import { TextInput } from 'ui/inputs';
import { FormField, Form } from 'ui/forms';
import Button from 'ui/buttons';
import { Link } from 'react-router';

import type { HTMLElement } from 'types';

type RestoreState = {
  emailSent: boolean;
  error: ?string;
  email: string;
};

/* ::`*/
@connect()
@cssModules(styles)
/* ::`*/
export default class RestorePassword extends Component {

  static propTypes = {
    fields: PropTypes.object.isRequired,
    handleSubmit: PropTypes.func.isRequired,
    resetForm: PropTypes.func.isRequired,
    submitting: PropTypes.bool.isRequired,
    error: PropTypes.string,
    dispatch: PropTypes.func,
  };

  state: RestoreState = {
    emailSent: false,
    error: null,
    email: '',
  };

  @autobind
  handleSubmit(): ?Promise {
    const { email } = this.state;

    if (email.endsWith('.com')) {
      this.setState({
        emailSent: true,
        error: null,
      });
    } else {
      this.setState({
        error: `Oops! We don’t have a user with that email. Please check your entry and try again.`,
      });

      return Promise.reject({
        email: 'A user with this email does not exist.',
      });
    }
  }

  get topMessage(): HTMLElement {
    const { emailSent, error, email } = this.state;

    if (error) {
      return (
        <div styleName="top-message-error">
          {error}
        </div>
      );
    }

    if (emailSent) {
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

  @autobind
  changeEmail({target}: any) {
    this.setState({
      email: target.value,
    });
  }

  get emailField(): ?HTMLElement {
    const { emailSent, email } = this.state;

    if (emailSent) return null;

    return (
      <FormField name="email" key="email" styleName="form-field">
        <TextInput placeholder="EMAIL" required type="email" value={email} onChange={this.changeEmail} />
      </FormField>
    );
  }

  @autobind
  gotoLogin() {
    this.props.dispatch(routeActions.push('/login'));
  }

  get primaryButton(): HTMLElement {
    const { emailSent } = this.state;

    if (emailSent) {
      return (
        <Button styleName="primary-button" onClick={this.gotoLogin}>BACK TO LOG IN</Button>
      );
    }

    return <Button styleName="primary-button" type="submit">SUBMIT</Button>;
  }

  get switchStage(): ?HTMLElement {
    const { emailSent } = this.state;

    if (!emailSent) {
      return (
        <div styleName="switch-stage">
          <Link to="/login">BACK TO LOG IN</Link>
        </div>
      );
    }
  }

  render(): HTMLElement {
    return (
      <div>
        <div styleName="title">FORGOT PASSWORD</div>
        {this.topMessage}
        <Form onSubmit={this.handleSubmit}>
          {this.emailField}
          {this.primaryButton}
        </Form>
        {this.switchStage}
      </div>
    );
  }
}
