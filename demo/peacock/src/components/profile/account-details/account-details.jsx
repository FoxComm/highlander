// @flow
import React, { Component } from 'react';

// libs
import { connect } from 'react-redux';
import Name from './name';

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
    <div styleName="buttons-footer">
      <Button styleName="link-button" onClick={this.handleChangePasswordClick}>CHANGE PASSWORD</Button>
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
      </div>
    );
  }
}

const mapStateToProps = (state) => {
  return {
    account: state.profile.account,
    nameModalVisible: state.profile.nameModalVisible,
  };
}

export default connect(mapStateToProps, actions)(AccountDetails);
