// @flow
import React, { Component } from 'react';
import styles from '../profile.css';
import { connect } from 'react-redux';
import type { Promise as PromiseType } from 'types/promise';
import { browserHistory } from 'lib/history';

import Block from '../common/block';
import Button from 'ui/buttons';
import { Link } from 'react-router';

import * as actions from 'modules/profile';

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

type DetailsProps = {
  account: Account|EmptyAccount,
  fetchAccount: () => PromiseType,
}

class Details extends Component {
  props: DetailsProps;

  componentWillMount() {
    this.props.fetchAccount();
  }

  handleChangePasswordClick() {
    browserHistory.push('/profile/password');
  }

  render() {
    const { account } = this.props;
    return (
      <Block title="My Details">
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
      </Block>
    );
  }
}

export default connect(mapStateToProps, actions)(Details);
