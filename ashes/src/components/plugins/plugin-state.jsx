/* @flow */

import React, { Component } from 'react';

import { Dropdown } from 'components/dropdown';
import ContentBox from 'components/content-box/content-box';

type Props = {
  currentValue: string,
  disabled?: bool,
};

const AVAILABLE_STATES = {
  active: [['inactive', 'Inactive']],
  inactive: [['active', 'Active']],
};

export default class PluginState extends Component {

  props: Props;

  get currentValue(): string {
    return this.props.currentValue;
  }

  render() {
     return (
      <ContentBox title="Plugin State">
        <Dropdown value={this.currentValue}
                  // onChange={(value) => this.handleDropdownChange(value)}
                  disabled={this.props.disabled}
                  items={AVAILABLE_STATES[this.currentValue]}
                  // changeable={false}
        />
      </ContentBox>
    );
  }
}
