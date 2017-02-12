// @flow
import _ from 'lodash';
import React, { Component } from 'react';
import styles from '../profile.css';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import { browserHistory } from 'lib/history';

import { Link } from 'react-router';
import Block from '../common/block';
import Button from 'ui/buttons';
import { TextInput } from 'ui/inputs';

import * as actions from 'modules/profile';

import type { Promise as PromiseType } from 'types/promise';
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

type EditEmailProps = {
  account: Account|{},
  fetchAccount: () => PromiseType,
  updateAccount: (payload: Object) => PromiseType,
  updateState: AsyncStatus,
}

type State = {
  email: string,
}

class EditEmail extends Component {
  static title = 'Edit email';

  props: EditEmailProps;
  state: State = {
    email: this.props.account.email || '',
  };

  componentWillMount() {
    this.props.fetchAccount();
  }

  @autobind
  handleEmailChange(event) {
    this.setState({
      email: event.target.value,
    });
  }

  @autobind
  handleSave() {
    this.props.updateAccount({
      email: this.state.email,
    }).then(() => {
      browserHistory.push('/profile');
    });
  }

  render() {
    return (
      <Block title={EditEmail.title}>
        <div styleName="section">Use this form to update your email address.</div>
        <TextInput
          styleName="text-input"
          value={this.state.email}
          onChange={this.handleEmailChange}
        />
        <div styleName="buttons-footer">
          <Button
            styleName="save-button"
            onClick={this.handleSave}
            isLoading={this.props.updateState.inProgress}
          >
            Save
          </Button>
          <Link styleName="link" to="/profile">Cancel</Link>
        </div>
      </Block>
    );
  }
}

export default connect(mapStateToProps, actions)(EditEmail);
