/* @flow */

import React from 'react';

// libs
import _ from 'lodash';

// components
import MultiSelectRow from '../table/multi-select-row';

type Props = {
  contentType: Object,
  columns: Columns,
  params: Object,
};

const ContentTypeTransactionRow = (props: Props) => {
  const { contentType, columns, params } = props;

  const setCellContents = (contentType: Object, field: string) => {
    const r = _.get(contentType, field, null);

    if (field == 'debit') return -r;
    else if (field == 'orderPayment') return _.get(r, 'cordReferenceNumber');

    return r;
  };

  return (
    <MultiSelectRow
      columns={columns}
      row={contentType}
      setCellContents={setCellContents}
      params={params}
    />
  );
};

export default ContentTypeTransactionRow;
