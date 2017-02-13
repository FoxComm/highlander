/* @flow */

import React from 'react';

import UsersTypeahead from './users-typeahead';

type Props = {
  suggestState: AsyncState,
  suggested: Array<TUser>,
  suggestCustomers: (token: string) => Promise,
  onSelect: (customers: Array<TUser>) => void,
};

export default (props: Props) => (
  <UsersTypeahead
    label="Customers"
    suggestUsers={props.suggestCustomers}
    suggested={props.suggested}
    suggestState={props.suggestState}
    onSelect={props.onSelect}
  />
);
