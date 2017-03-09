// @flow
import _ from 'lodash';
import moment from 'moment';
import React from 'react';
import { isActive, setFromTo } from 'paragons/common';

import { Dropdown } from '../dropdown';

const SELECT_STATE = [
  ['active', 'Active'],
  ['inactive', 'Inactive'],
];


export type StateChangeEvent = {
  attributes: Attributes,
  activeFrom: ?string,
  activeTo: ?string,
  isActive: boolean,
}

type Props = {
  attributes: Attributes,
  onChange: (event: StateChangeEvent) => void,
}

function now(): string {
  return moment().utc().format('YYYY-MM-DDTHH:mm:ss.SSS[Z]');
}

const StateDropdown = (props: Props) => {
  const { attributes, onChange, ...rest } = props;

  const handleChange = (value: string) => {
    const isActive = value == 'active';
    const activeFrom = isActive ? now() : null;
    const activeTo = null;

    const event: StateChangeEvent = {
      get attributes() {
        return setFromTo(attributes, activeFrom, activeTo);
      },
      activeFrom,
      activeTo,
      isActive,
    };
    onChange(event);
  };

  const activeFrom = _.get(attributes, 'activeFrom.v');
  const activeTo = _.get(attributes, 'activeTo.v');

  const value = isActive(activeFrom, activeTo) ? 'active' : 'inactive';

  return (
    <Dropdown
      {...rest}
      value={value}
      onChange={handleChange}
      items={SELECT_STATE}
    />
  );
};

export default StateDropdown;
