// libs
import React from 'react';
import { Dropdown } from '../dropdown';

type Props = {
  value: boolean,
  onChange: Function,
}

const BOOL_OPTIONS = [
  [true, 'Yes'],
  [false, 'No'],
];

export const BooleanOptions = (props: Props) => {
  return (
    <Dropdown
      value={props.value}
      items={BOOL_OPTIONS}
      onChange={props.onChange}
    />
  );
};