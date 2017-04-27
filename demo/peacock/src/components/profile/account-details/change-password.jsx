// @flow

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import { browserHistory } from 'lib/history';

// components
import { Link } from 'react-router';
import Button from 'ui/buttons';
import ShowHidePassword from 'ui/forms/show-hide-password';
import { Form, FormField } from 'ui/forms';
import ErrorAlerts from '@foxcomm/wings/lib/ui/alerts/error-alerts';

// styles
import styles from '../profile.css';

import * as actions from 'modules/profile';

import type { AsyncStatus } from 'types/async-actions';

function mapStateToProps(state) {
  return {
    account: state.profile.account,
    changeState: _.get(state.asyncActions, 'changePassword', {}),
  };
}

type Account = {
  name: string,
  email: string,
  isGuest: boolean,
  id: number,
};

type ChangePasswordProps = {
  account: Account|{},
  fetchAccount: () => Promise<*>,
  changePassword: (oldPassword: string, newPassword: string) => Promise<*>,
  changeState: AsyncStatus,
};

type State = {
  currentPassword: string,
  newPassword1: string,
  newPassword2: string,
  error: any,
};

class ChangePassword extends Component {
  static title = 'Change password';

  props: ChangePasswordProps;
  state: State = {
    currentPassword: '',
    newPassword1: '',
    newPassword2: '',
    error: null,
  };

  @autobind
  handleFormChange(event) {
    const { target } = event;
    this.setState({
      [target.name]: target.value,
      error: null,
    });
  }

  @autobind
  handleSave() {
    const { newPassword1, newPassword2, currentPassword } = this.state;

    if (newPassword1 != newPassword2) {
      return Promise.reject({
        newPassword2: 'Your passwords must match.',
      });
    }

    if (currentPassword == newPassword1) {
      return Promise.reject({
        newPassword1: 'Your new password must be different than your old password.',
      });
    }

    this.props.changePassword(
      currentPassword,
      newPassword1
    ).then(() => {
      browserHistory.push('/profile');
    }).catch((err) => {
      this.setState({error: err});
    });
  }

  render() {
    return (
      <div>
        <div styleName="section">Use this form to change your password.</div>
        <Form onChange={this.handleFormChange} onSubmit={this.handleSave}>
          <FormField name="currentPassword" styleName="form-field" required>
            <ShowHidePassword
              placeholder="CURRENT PASSWORD"
              styleName="text-input"
              value={this.state.currentPassword}
              name="currentPassword"
              required
            />
          </FormField>
          <FormField name="newPassword1" styleName="form-field" required>
            <ShowHidePassword
              placeholder="NEW PASSWORD"
              styleName="text-input"
              value={this.state.newPassword1}
              name="newPassword1"
              minLength={8}
              required
            />
          </FormField>
          <FormField name="newPassword2" styleName="form-field" required>
            <ShowHidePassword
              placeholder="RETYPE NEW PASSWORD"
              styleName="text-input"
              value={this.state.newPassword2}
              name="newPassword2"
              minLength={8}
              required
            />
          </FormField>
          <ErrorAlerts error={this.state.error} />
          <div styleName="buttons-footer">
            <Button
              type="submit"
              styleName="save-button"
              isLoading={this.props.changeState.inProgress}
            >
              Save
            </Button>
            <Link styleName="link" to="/profile">Cancel</Link>
          </div>
        </Form>
      </div>
    );
  }
}

export default connect(mapStateToProps, actions)(ChangePassword);
