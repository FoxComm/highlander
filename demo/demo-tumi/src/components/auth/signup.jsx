/* @flow */

import _ from 'lodash';
import React, { Component, Element } from 'react';
import styles from './auth.css';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { Link } from 'react-router';

import { browserHistory } from 'lib/history';

import localized from 'lib/i18n';
import type { Localized } from 'lib/i18n';

import TextInput from 'ui/text-input/text-input';
import { FormField, Form } from 'ui/forms';
import Button from 'ui/buttons';
import ErrorAlerts from 'ui/alerts/error-alerts';

import * as actions from 'modules/auth';
import { authBlockTypes } from 'paragons/auth';
import { fetch as fetchCart, saveLineItemsAndCoupons } from 'modules/cart';

import type { SignUpPayload } from 'modules/auth';

type AuthState = {
  email: string,
  firstName: string,
  lastName: string,
  password: string,
  passwordConfirm: string,
  firstName: string,
  lastName: string,
  usernameError: bool | string,
  emailError: bool | string,
  generalErrors: Array<string>,
  passwordConfirm: string,
};

type Props = Localized & {
  getPath: Function,
  isLoading: boolean,
  fetchCart: Function,
  saveLineItemsAndCoupons: Function,
  onLoginClick: Function,
  title?: string | Element<*> | null,
  mergeGuestCart: boolean,
  onAuthenticated?: Function,
};

const mapState = state => ({
  cart: state.cart,
  isLoading: _.get(state.asyncActions, ['auth-signup', 'inProgress'], false),
});

class Signup extends Component {
  props: Props;

  state: AuthState = {
    email: '',
    password: '',
    username: '',
    passwordConfirm: '',
    firstName: '',
    lastName: '',
    usernameError: false,
    emailError: false,
    generalErrors: [],
  };

  static defaultProps = {
    mergeGuestCart: false,
  };

  @autobind
  onChangeEmail({ target }: any) {
    this.setState({
      email: target.value,
      emailError: false,
    });
  }

  @autobind
  onChangePassword({ target }: any) {
    this.setState({
      password: target.value,
    });
  }

  @autobind
  onChangePasswordConfirm({ target }: any) {
    this.setState({
      passwordConfirm: target.value,
    });
  }

  @autobind
  onChangeFirstName({ target }: any) {
    this.setState({
      firstName: target.value,
    });
  }

  @autobind
  onChangeLastName({ target }: any) {
    this.setState({
      lastName: target.value,
    });
  }

  @autobind
  submitUser() {
    const { email, password, firstName, lastName } = this.state;
    const payload: SignUpPayload = { email, password, firstName, lastName };
    const signUp = this.props.signUp(payload).then(() => {
      const lineItems = _.get(this.props, 'cart.lineItems', []);
      const couponCode = _.get(this.props, 'cart.coupon.code', null);
      if (_.isEmpty(lineItems) && _.isNil(couponCode)) {
        this.props.fetchCart();
      } else {
        this.props.saveLineItemsAndCoupons(this.props.mergeGuestCart);
      }
      browserHistory.push(this.props.getPath());
      this.props.toggleAuthMenu();
    }).catch((err) => {
      const errors = _.get(err, ['responseJson', 'errors'], [err.toString()]);
      let emailError = false;
      let usernameError = false;

      const restErrors = _.reduce(errors, (acc, error) => {
        if (error.indexOf('email') >= 0) {
          emailError = error;
        } else if (error.tolowerCase().indexOf('name') >= 0) {
          usernameError = error;
        } else {
          return [...acc, error];
        }

        return acc;
      }, []);

      this.setState({
        emailError,
        usernameError,
        generalErrors: restErrors,
      });
    });

    if (this.props.onAuthenticated) {
      signUp.then(this.props.onAuthenticated);
    }
  }

  get bottomMessage() {
    const { props } = this;
    const { t } = props;

    return (
      <div styleName="bottom-message">
        <div styleName="title">{t('Already have an account?')}</div>
        <Link to={props.getPath(authBlockTypes.LOGIN)} onClick={props.onLoginClick} styleName="link">
          {t('Sign in')}
        </Link>
      </div>
    );
  }

  render(): Element<*> {
    const { email, password, passwordConfirm, firstName, lastName, emailError, usernameError } = this.state;
    const { t, isLoading } = this.props;

    return (
      <div>
        <Form onSubmit={this.submitUser}>
          <div styleName="inputs-body">
            <ErrorAlerts errors={this.state.generalErrors} />
            <FormField key="email" styleName="form-field" error={emailError}>
              <TextInput
                required
                label={t('Email Address')}
                name="email"
                value={email}
                type="email"
                onChange={this.onChangeEmail}
              />
            </FormField>
            <FormField key="passwd" styleName="form-field" required>
              <TextInput
                type="password"
                className={styles['form-field-input']}
                label={t('Password')}
                name="password"
                value={password}
                onChange={this.onChangePassword}
              />
            </FormField>
            <FormField key="passwd-confirm" styleName="form-field" required>
              <TextInput
                type="password"
                className={styles['form-field-input']}
                label={t('Confirm Password')}
                name="password-confirm"
                value={passwordConfirm}
                onChange={this.onChangePasswordConfirm}
              />
            </FormField>
            <FormField key="firstname" styleName="form-field" error={usernameError}>
              <TextInput
                required
                label={t('First Name')}
                name="firstName"
                value={firstName}
                onChange={this.onChangeFirstName}
              />
            </FormField>
            <FormField key="lastname" styleName="form-field" error={usernameError}>
              <TextInput
                required
                label={t('Last Name')}
                name="lastName"
                value={lastName}
                onChange={this.onChangeLastName}
              />
            </FormField>
          </div>
          <Button
            styleName="link"
            isLoading={isLoading}
            type="submit"
            children={t('Create Account')}
          />
          {this.bottomMessage}
        </Form>
      </div>
    );
  }
}

export default connect(mapState, {
  ...actions,
  fetchCart,
  saveLineItemsAndCoupons,
})(localized(Signup));
