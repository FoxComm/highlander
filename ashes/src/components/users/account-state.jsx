/* @flow */

// libs
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';

// components
import { TextDropdown } from 'components/core/dropdown';
import ContentBox from '../content-box/content-box';
import ConfirmationModal from 'components/core/confirmation-modal';

// actions
import * as UserActions from '../../modules/users/details';

const SELECT_STATE = [
  ['active', 'Active'],
  ['inactive', 'Inactive'],
  ['archived', 'Archived'],
  ['invited', 'Invited', true],
];

type Props = {
  disabled: bool,
  onChange: Function,
  currentValue: string,
  updateAccountState: Function,
  userId: number | string,
};

type State = {
  newState: any
};

class AccountState extends Component {
  props: Props;

  state: State = {
    newState: null,
  };

  handleDropdownChange(value: string) {
    this.setState({
      newState: value,
    });
  }

  @autobind
  confirmStateChange() {
    this.props.updateAccountState(this.props.userId, this.state.newState);
    this.restoreState();
  }

  @autobind
  restoreState() {
    this.setState({
      newState: null
    });
  }

  render() {
    const text = `Are you sure you want to change account state to ${this.state.newState} ?`;
    const strongText = 'You won\'t be able to change it back!';
    let confirmation;
    if (this.state.newState === 'archived') {
      confirmation = (
        <div>
          <p>{text}</p>
          <strong>{strongText}</strong>
        </div>
      );
    } else {
      confirmation = text;
    }

    return (
      <div>
        <ContentBox title="Account State">
          <TextDropdown
            value={this.props.currentValue}
            onChange={(value) => this.handleDropdownChange(value)}
            disabled={this.props.disabled}
            items={SELECT_STATE}
            stateless
          />
        </ContentBox>
        <ConfirmationModal
          isVisible={this.state.newState !== null}
          title="Change Account State ?"
          confirmLabel="Yes, Change"
          onCancel={this.restoreState}
          onConfirm={this.confirmStateChange}
        >
          {confirmation}
        </ConfirmationModal>
      </div>
    );
  }
}

export default connect(
  null,
  UserActions
)(AccountState);
