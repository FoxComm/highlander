/* @flow */

//libs
import React, { Component } from 'react';

//components
import EditGroupBase from './edit-group-base';

type Props = {
  group: TCustomerGroup;
  onSave: () => Promise;
}

export default (props: Props) => (
  <EditGroupBase
    title="New Customer Group"
    cancelTo="customer-groups"
    {...props}
  />
);
