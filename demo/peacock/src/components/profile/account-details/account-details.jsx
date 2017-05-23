/* @flow */

import React, { Component } from 'react';

// libs
import { connect } from 'react-redux';
import _ from 'lodash';

// components
import EditEmail from './edit-email';
import EditName from './edit-name';
import ChangePassword from './change-password';
import DetailsBlock from '../details-block';
import Loader from 'ui/loader';

// actions
import * as actions from 'modules/profile';

// types
import type { AccountDetailsProps } from 'types/profile';
import type { AsyncStatus } from 'types/async-actions';

import styles from '../profile.css';

type Props = AccountDetailsProps & {
  fetchAccount: () => Promise<*>,
  className?: string,
  nameModalVisible: boolean,
  emailModalVisible: boolean,
  passwordModalVisible: boolean,
  fetchAccountState: AsyncStatus,
}

class AccountDetails extends Component {
  props: Props;

  componentWillMount() {
    this.props.fetchAccount();
  }

  get nameModalContent() {
    const { account } = this.props;

    return (
      <EditName
        name={account.name}
      />
    );
  }

  get emailModalContent() {
    const { account } = this.props;

    return (
      <EditEmail
        email={account.email}
      />
    );
  }

  get passwordModalContent() {
    const { account } = this.props;

    return (
      <ChangePassword
        account={account}
      />
    );
  }

  get nameData() {
    const { account, fetchAccountState } = this.props;

    if (!fetchAccountState) return <Loader size="m" />;

    return account.name;
  }

  get emailData() {
    const { account, fetchAccountState } = this.props;

    if (!fetchAccountState) return <Loader size="m" />;

    return account.email;
  }

  render() {
    const { props } = this;
    return (
      <div className={props.className}>
        <DetailsBlock
          data={this.nameData}
          toggleModal={props.toggleNameModal}
          modalVisible={props.nameModalVisible}
          actionTitle="Edit"
          modalContent={this.nameModalContent}
          blockTitle="First and last name"
        />
        <DetailsBlock
          data={this.emailData}
          toggleModal={props.toggleEmailModal}
          modalVisible={props.emailModalVisible}
          actionTitle="Edit"
          modalContent={this.emailModalContent}
          blockTitle="Email"
        />
        <DetailsBlock
          toggleModal={props.togglePasswordModal}
          modalVisible={props.passwordModalVisible}
          actionTitle="Change"
          modalContent={this.passwordModalContent}
          blockTitle="Password"
        />
      </div>
    );
  }
}

const mapStateToProps = (state) => {
  return {
    account: _.get(state.profile, 'account', {}),
    nameModalVisible: _.get(state.profile, 'nameModalVisible', false),
    emailModalVisible: _.get(state.profile, 'emailModalVisible', false),
    passwordModalVisible: _.get(state.profile, 'passwordModalVisible', false),
    fetchAccountState: _.get(state.asyncActions, 'fetchAccount.finished', false),
  };
};

export default connect(mapStateToProps, {
  ...actions,
})(AccountDetails);
