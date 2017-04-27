// @flow
import React, { Component } from 'react';

// libs
import { connect } from 'react-redux';
import _ from 'lodash';

// components
import Name from './name';
import Email from './email';
import Password from './password';

import * as actions from 'modules/profile';

import type { AccountDetailsProps } from 'types/profile';

import styles from './account-details.css';

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

  render() {
    const { props } = this;
    return (
      <div className={props.className}>
        <Name
          name={props.account.name}
          toggleModal={props.toggleNameModal}
          modalVisible={props.nameModalVisible}
        />
        <Email
          email={props.account.email}
          toggleModal={props.toggleEmailModal}
          modalVisible={props.emailModalVisible}
        />
        <Password
          account={props.account}
          toggleModal={props.togglePasswordModal}
          modalVisible={props.passwordModalVisible}
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
