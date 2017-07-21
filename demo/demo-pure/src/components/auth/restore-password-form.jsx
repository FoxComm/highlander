/* @flow */

import _ from 'lodash';
import React, { Component, Element } from 'react';
import styles from './auth.css';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { Link } from 'react-router';

import { browserHistory } from 'lib/history';

import { authBlockTypes } from 'paragons/auth';

import localized from 'lib/i18n';

import { TextInput } from 'ui/text-input';
import { FormField, Form } from 'ui/forms';
import Button from 'ui/buttons';

import { restorePassword } from 'modules/auth';

type RestoreState = {
  emailSent: boolean;
  error: ?string;
  email: string;
};

export type RestorePasswordFormProps = {
  getPath: Function,
};

type RestorePasswordFormPropsFinal = RestorePasswordFormProps & {
  topMessage: string,
  title: string,
  restorePassword: Function,
  t: Function,
}

class RestorePasswordForm extends Component {
  props: RestorePasswordFormPropsFinal;

  state: RestoreState = {
    emailSent: false,
    error: null,
    email: '',
  };

  @autobind
  handleSubmit(): ?Promise<*> {
    const { email } = this.state;
    const { t } = this.props;

    if (_.isEmpty(email)) {
      return Promise.reject({
        email: t('Oops! We don’t have a user with that email. Please check your entry and try again.'),
      });
    }

    return this.props.restorePassword(email)
      .then(() => {
        this.setState({
          emailSent: true,
          error: null,
        });
      }).catch(() => {
        this.setState({
          error: t('Oops! We don’t have a user with that email. Please check your entry and try again.'),
        });
      }
    );
  }

  get topMessage(): Element<*> {
    const { emailSent, error, email } = this.state;
    const { t } = this.props;

    if (error) {
      return (
        <div styleName="top-message">
          {error}
        </div>
      );
    }

    if (emailSent) {
      return (
        <div styleName="top-message">
          {t('An email was successfully sent to')} <strong>{email}</strong> {t('with reset instructions')}!
        </div>
      );
    }

    return (
      <div styleName="top-message">
        {this.props.topMessage}
      </div>
    );
  }

  @autobind
  changeEmail({target}: any) {
    this.setState({
      email: target.value,
    });
  }

  get emailField(): ?Element<*> {
    const { emailSent, email } = this.state;
    const { t } = this.props;

    if (emailSent) return null;

    return (
      <FormField name="email" key="email" styleName="form-field">
        <TextInput placeholder={t('Email')} required type="email" value={email} onChange={this.changeEmail} />
      </FormField>
    );
  }

  goToLogin: Object = () => {
    browserHistory.push(this.props.getPath(authBlockTypes.LOGIN));
  };

  get primaryButton(): Element<*> {
    const { emailSent } = this.state;
    const { t } = this.props;

    if (emailSent) {
      return (
        <Button styleName="primary-button" onClick={this.goToLogin} type="button">
          {t('Back to sign in')}
        </Button>
      );
    }

    return <Button styleName="primary-button" type="submit">{t('SUBMIT')}</Button>;
  }

  get switchStage(): ?Element<*> {
    const { emailSent } = this.state;
    const { t, getPath } = this.props;

    if (!emailSent) {
      return (
        <div styleName="bottom-message">
          <Link to={getPath(authBlockTypes.LOGIN)} styleName="link">
            {t('Back to sign in')}
          </Link>
        </div>
      );
    }
  }

  render(): Element<*> {
    return (
      <div>
        <div styleName="title">{this.props.title}</div>
        {this.topMessage}
        <Form onSubmit={this.handleSubmit}>
          <div styleName="inputs-body">
            {this.emailField}
          </div>
          {this.primaryButton}
        </Form>
        {this.switchStage}
      </div>
    );
  }
}

export default _.flowRight(
  connect(null, { restorePassword }),
  localized
)(RestorePasswordForm);
