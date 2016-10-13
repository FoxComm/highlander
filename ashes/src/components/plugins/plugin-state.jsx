/* @flow */

import React, { Component, Element } from 'react';

import { Dropdown } from 'components/dropdown';
import ContentBox from 'components/content-box/content-box';

type Props = {
  currentValue: string,
  handleDropdownChange: Function,
};

const AVAILABLE_STATES = [
  ['inactive', 'Inactive'],
  ['active', 'Active']
];

const PluginState = (props: Props): Element => {

  const { currentValue, handleDropdownChange } = props;

  return (
    <ContentBox title="Plugin State">
      <Dropdown
        value={currentValue}
        onChange={(value) => handleDropdownChange(value)}
        items={AVAILABLE_STATES}
      />
    </ContentBox>
  );
}

export default PluginState;
