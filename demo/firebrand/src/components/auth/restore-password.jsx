/* @flow */

import React, { Component, PropTypes } from 'react';
import styles from './auth.css';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { browserHistory, Link } from 'react-router';

import { authBlockTypes } from 'paragons/auth';

import localized from 'lib/i18n';

import { TextInput } from 'ui/inputs';
import { FormField, Form } from 'ui/forms';
import Button from 'ui/buttons';

import type { HTMLElement } from 'types';

type RestoreState = {
  emailSent: boolean;
  error: ?string;
  email: string;
};

/* ::`*/
@connect()
@localized
/* ::`*/
export default class RestorePassword extends Component {

  static propTypes = {
    fields: PropTypes.object.isRequired,
    handleSubmit: PropTypes.func.isRequired,
    resetForm: PropTypes.func.isRequired,
    submitting: PropTypes.bool.isRequired,
    error: PropTypes.string,
    dispatch: PropTypes.func,
    changeAuthBlockType: PropTypes.func,
    getPath: PropTypes.func,
  };

  state: RestoreState = {
    emailSent: false,
    error: null,
    email: '',
  };

  @autobind
  handleSubmit(): ?Promise {
    const { email } = this.state;
    const { t } = this.props;

    if (email.endsWith('.com')) {
      this.setState({
        emailSent: true,
        error: null,
      });
    } else {
      this.setState({
        error: t(`Oops! We don’t have a user with that email. Please check your entry and try again.`),
      });

      return Promise.reject({
        email: t('A user with this email does not exist.'),
      });
    }
  }

  get topMessage(): HTMLElement {
    const { emailSent, error, email } = this.state;
    const { t } = this.props;

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
          {t('An email with reset instructions was successfully sent to')} <strong>{email}</strong>!
        </div>
      );
    }

    return (
      <div styleName="top-message">
        {t('No worries! We’ll email you instructions on how to reset your password.')}
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
    const { t } = this.props;

    if (emailSent) return null;

    return (
      <FormField name="email" key="email" styleName="form-field">
        <TextInput placeholder={t('EMAIL')} required type="email" value={email} onChange={this.changeEmail} />
      </FormField>
    );
  }

  goToLogin: Object = () => {
    browserHistory.push(this.props.getPath(authBlockTypes.LOGIN));
  };

  get primaryButton(): HTMLElement {
    const { emailSent } = this.state;
    const { t } = this.props;

    if (emailSent) {
      return (
        <Button styleName="primary-button" onClick={this.goToLogin} type="button">
          {t('BACK TO LOG IN')}
        </Button>
      );
    }

    return <Button styleName="primary-button" type="submit">{t('SUBMIT')}</Button>;
  }

  get switchStage(): ?HTMLElement {
    const { emailSent } = this.state;
    const { t, getPath } = this.props;

    if (!emailSent) {
      return (
        <div styleName="switch-stage">
          <Link to={getPath(authBlockTypes.LOGIN)} styleName="link">
            {t('BACK TO LOG IN')}
          </Link>
        </div>
      );
    }
  }

  render(): HTMLElement {
    const { t } = this.props;

    return (
      <div>
        <div styleName="title">{t('FORGOT PASSWORD')}</div>
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
