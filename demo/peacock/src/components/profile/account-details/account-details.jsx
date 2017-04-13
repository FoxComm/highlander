// @flow
import React, { Component } from 'react';

// libs
import { connect } from 'react-redux';
import Name from './name';

import * as actions from 'modules/profile';

import styles from './account-details.css';

function mapStateToProps(state) {
  return {
    account: state.profile.account,
  };
}

type Account = {
  name: string,
  email: string,
  isGuest: boolean,
  id: number,
}

type EmptyAccount = {
  name?: string,
  email?: string,
}

type AccountDetailsProps = {
  account: Account|EmptyAccount,
  fetchAccount: () => Promise<*>,
  className?: string,
}

class AccountDetails extends Component {
  props: AccountDetailsProps;

  componentWillMount() {
    this.props.fetchAccount();
  }

  render() {
    /*
    <div styleName="section">
      <div styleName="line">
        <div styleName="subtitle">First and last name</div>
        <Link styleName="link" to="/profile/name">EDIT</Link>
      </div>
      <div styleName="value">{account.name}</div>
    </div>
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
    const { account, className } = this.props;
    return (
      <div className={className}>
        <Name
          name={account.name}
        />
      </div>
    );
  }
}

export default connect(mapStateToProps, actions)(AccountDetails);
