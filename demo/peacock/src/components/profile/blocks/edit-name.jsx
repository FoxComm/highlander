// @flow
import _ from 'lodash';
import React, { Component } from 'react';
import styles from '../profile.css';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import { browserHistory } from 'lib/history';
import { clearErrorsFor } from '@foxcomm/wings';

import { Link } from 'react-router';
import Button from 'ui/buttons';
import { TextInput } from 'ui/text-input';
import { FormField, Form } from 'ui/forms';
import ErrorAlerts from '@foxcomm/wings/lib/ui/alerts/error-alerts';

import * as actions from 'modules/profile';

import type { AsyncStatus } from 'types/async-actions';

function mapStateToProps(state) {
  return {
    account: state.profile.account,
    updateState: _.get(state.asyncActions, 'updateAccount', {}),
  };
}

type Account = {
  name: string,
  email: string,
  isGuest: boolean,
  id: number,
}

type EmptyAccount = {
  email: void,
  name: void,
}

type EditNameProps = {
  account: Account|EmptyAccount,
  fetchAccount: () => Promise<*>,
  updateAccount: (payload: Object) => Promise<*>,
  updateState: AsyncStatus,
  clearErrorsFor: (...args: Array<string>) => void,
}

type State = {
  name: string,
}

class EditName extends Component {
  static title = 'Edit first & last name';

  props: EditNameProps;
  state: State = {
    name: this.props.account.name || '',
  };

  componentWillMount() {
    this.props.fetchAccount();
  }

  componentWillUnmount() {
    this.props.clearErrorsFor('updateAccount');
  }

  @autobind
  handleNameChange(event) {
    this.setState({
      name: event.target.value,
    });
  }

  @autobind
  handleSave() {
    this.props.updateAccount({
      name: this.state.name,
    }).then(() => {
      browserHistory.push('/profile');
    });
  }

  render() {
    return (
      <div>
        <Form onSubmit={this.handleSave}>
          <div styleName="section">Use this form to update your first and last name.</div>
          <FormField error={!!this.props.updateState.err}>
            <TextInput
              required
              value={this.state.name}
              onChange={this.handleNameChange}
            />
          </FormField>
          <ErrorAlerts
            error={this.props.updateState.err}
          />
          <div styleName="buttons-footer">
            <Button
              type="submit"
              styleName="save-button"
              isLoading={this.props.updateState.inProgress}
              children="Save"
            />
            <Link styleName="link" to="/profile">Cancel</Link>
          </div>
        </Form>
      </div>
    );
  }
}

export default connect(mapStateToProps, {...actions, clearErrorsFor})(EditName);
