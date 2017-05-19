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

// actions
import * as actions from 'modules/profile';

import type { AccountDetailsProps } from 'types/profile';

import styles from '../profile.css';

type Props = AccountDetailsProps & {
  fetchAccount: () => Promise<*>,
  className?: string,
  nameModalVisible: boolean,
}

class AccountDetails extends Component {
  props: Props;

  componentWillMount() {
    this.props.fetchAccount();
  }

  get nameModalContent() {
    return (
      <EditName />
    );
  }

  get emailModalContent() {
    return (
      <EditEmail />
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

  render() {
    const { props } = this;
    return (
      <div className={props.className}>
        <DetailsBlock
          data={props.account.name}
          toggleModal={props.toggleNameModal}
          modalVisible={props.nameModalVisible}
          actionTitle="Edit"
          modalContent={this.nameModalContent}
          blockTitle="First and last name"
        />
        <DetailsBlock
          data={props.account.email}
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
  };
};

export default connect(mapStateToProps, {
  ...actions,
})(AccountDetails);
