// @flow
import React, { Component } from 'react';

// libs
import { connect } from 'react-redux';

// components
import Name from './name';
import Email from './email';

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
    /*
    <div styleName="section">
    <div styleName="section">
      <div styleName="line">
        <div styleName="subtitle">Email</div>
        <Link styleName="link" to="/profile/email">EDIT</Link>
      </div>
      <div styleName="value">{account.email}</div>
    </div>
    */
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
      </div>
    );
  }
}

const mapStateToProps = (state) => {
  return {
    account: state.profile.account,
    nameModalVisible: state.profile.nameModalVisible,
    emailModalVisible: state.profile.emailModalVisible,
  };
};

export default connect(mapStateToProps, actions)(AccountDetails);
