/* @flow */

//libs
import _ from 'lodash';
import React, { Component } from 'react';

//components
import EditGroupBase from './edit-group-base';

type Props = {
  params: {
    type: string;
  };
  group: TCustomerGroup;
  onSave: () => Promise;
};

export default (props: Props) => {
  const type = _.get(props, 'params.type');
  const title = `New ${_.capitalize(type)} Customer Group`;

  return (
    <EditGroupBase
      title={title}
      cancelTo="customer-groups"
      {...props}
    />
  );
};
