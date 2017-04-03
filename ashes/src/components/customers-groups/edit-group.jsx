/* @flow */

//libs
import React from 'react';

//components
import EditGroupBase from './edit-group-base';

type Props = {
  group: TCustomerGroup,
  title: string,
  onSave: () => Promise<*>,
  saveInProgress: boolean,
  saveError: boolean,
  params: {
    type: string,
  },
};

export default (props: Props) => (
  <EditGroupBase
    title="Edit Customer Group"
    cancelTo="customer-group"
    cancelParams={{ groupId: props.group.id }}
    {...props}
  />
);
