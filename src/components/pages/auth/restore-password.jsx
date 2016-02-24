/* @flow */

import React, { Component, Element, PropTypes } from 'react';
import cssModules from 'react-css-modules';
import styles from './auth.css';
import { autobind } from 'core-decorators';
import {reduxForm} from 'redux-form';

import { TextInput } from '../../common/inputs';
import { FormField } from '../../forms';
import Button from '../../common/buttons';
import { Link } from 'react-router';

type FormData = {
  email: string;
};

type RestoreState = {
  sent: boolean
};

/* ::`*/
@reduxForm({
  form: 'restore-password',
  fields: ['email'],
})
@cssModules(styles)
/* ::`*/
export default class RestorePassword extends Component {

  static propTypes = {
    fields: PropTypes.object.isRequired,
    handleSubmit: PropTypes.func.isRequired,
    resetForm: PropTypes.func.isRequired,
    submitting: PropTypes.bool.isRequired,
  };

  state: RestoreState = {
    sent: false,
  };

  @autobind
  onChangeEmail({target}: SEvent<HTMLInputElement>) {
    this.setState({
      email: target.value,
    });
  }

  @autobind
  handleSubmit(data: FormData) {
    if (data.email.endsWith('.com')) {
      this.setState({
        sent: true,
      });
    } else {
      return Promise.reject({
        email: 'A user with this email does not exist.',
      });
    }
  }

  get topMessage(): Element {
    const { sent } = this.state;
    const { fields: {email}} = this.props;

    if (sent) {
      return (
        <div styleName="top-message-success">
          An email was successfully sent to <strong>{email.value}</strong> with reset instructions!
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
    const { sent } = this.state;
    const { fields: {email}} = this.props;

    if (sent) return null;

    return (
      <FormField key="email" styleName="form-field" {...email}>
        <TextInput placeholder="EMAIL" required type="email" {...email} />
      </FormField>
    );
  }

  render(): Element {
    const {handleSubmit} = this.props;

    return (
      <div>
        <div styleName="title">FORGOT PASSWORD</div>
        {this.topMessage}
        <form onSubmit={handleSubmit(this.handleSubmit)}>
          {this.emailField}
          <Button styleName="primary-button">SUBMIT</Button>
        </form>
        <div styleName="switch-stage">
          <Link to="/login">BACK TO LOG IN</Link>
        </div>
      </div>
    );
  }
}
