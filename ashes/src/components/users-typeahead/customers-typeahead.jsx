/* @flow */

import React from 'react';

import UsersTypeahead from './users-typeahead';

type Props = {
  suggestState: string,
  suggested: Array<Customer>,
  suggestCustomers: (token: string) => Array<Customer>,
  onSelect: (customers: Array<Customer>) => void,
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
