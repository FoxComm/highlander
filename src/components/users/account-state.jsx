/* @flow */

// libs
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';

// components
import { Dropdown } from '../dropdown';
import ContentBox from '../content-box/content-box';
import ConfirmationDialog from '../modal/confirmation-dialog';

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
};

type State = {
  currentState: string,
  newState: any
};

class AccountState extends Component {
  props: Props;

  state: State = {
    currentState: this.props.currentValue,
    newState: null,
  };

  handleDropdownChange(value: string) {
    console.log(value);
    this.setState({
      newState: value,
    });
  }

  @autobind
  confirmStateChange() {
    this.setState({
      currentState: this.state.newState,
      newState: null
    });
  }

  @autobind
  cancelStateChange() {
    this.setState({
      newState: null
    });
  }

  render(): Element {
    const text = `Are you sure you want to change account state to ${this.state.newState} ?`;
    let confirmation;
    if (this.state.newState === 'archived') {
      confirmation = (
        <div>
          <p>{text}</p>
          <strong>You won't be able to change it back!</strong>
        </div>
      );
    } else {
      confirmation = text;
    }

    return (
      <div>
        <ContentBox title="Account State">
          <Dropdown value={this.state.currentState}
                    onChange={(value) => this.handleDropdownChange(value)}
                    disabled={this.props.disabled}
                    items={SELECT_STATE}
                    changeable={false}
          />
        </ContentBox>
        <ConfirmationDialog
          isVisible={this.state.newState != null}
          header="Change Account State ?"
          body={confirmation}
          cancel="Cancel"
          confirm="Yes, Change"
          cancelAction={this.cancelStateChange}
          confirmAction={this.confirmStateChange}
        />
      </div>
    );
  }
}

export default AccountState;