/* @flow */

//libs
import React, { Component } from 'react';

//components
import EditGroupBase from './edit-group-base';

type Props = {
  group: TCustomerGroup;
  onSave: () => Promise;
  fetchGroup: (id: number) => Promise;
  fetchRegions: () => Promise;
}

export default (props: Props) => (
  <EditGroupBase
    title="Edit Customer Group"
    cancelTo="customer-group"
    cancelParams={{groupId: props.group.id}}
    {...props}
  />
);
