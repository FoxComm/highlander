/* @flow */

//libs
import _ from 'lodash';
import React, { Component } from 'react';

//components
import EditGroupBase from './edit-group-base';

type Props = {
  group: TCustomerGroup,
  onSave: () => Promise,
  saveInProgress: boolean,
  saveError: boolean,
  params: {
    type: string,
  },
};

export default (props: Props) => {
  const type = _.get(props, 'params.type');
  const title = `New ${_.capitalize(type)} Customer Group`;

  return (
    <EditGroupBase
      title={title}
      cancelTo="customer-groups"
      cancelParams={{/* flow fix */}}
      {...props}
    />
  );
};
