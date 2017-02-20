/* @flow */

//libs
import React, { Component, Element } from 'react';
import _ from 'lodash';

// components
import RoundedPill from '../rounded-pill/rounded-pill';
import MultiSelectRow from '../table/multi-select-row';

// types
import type { MerchantApplication } from 'paragons/merchant-application';

type Props = {
  application: MerchantApplication,
  columns?: Array<Object>,
  params: Object,
};

function setCellContents(application, field) {
  switch (field) {
    case 'state':
      return <RoundedPill text={application.state} />;
    default:
      return _.get(application, field);
  }
}

export default class MerchantApplicationRow extends Component {
  props: Props;

  render() {
    const { application, columns, params } = this.props;
    const commonParams = {
      columns,
      row: application,
      setCellContents,
      params,
    };

    return (
      <MultiSelectRow
        { ...commonParams }
        linkTo="application-details"
        linkParams={{applicationId: application.id}} />
    );

  }
}
