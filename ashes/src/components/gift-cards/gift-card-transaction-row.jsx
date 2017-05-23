/* @flow */

import React from 'react';

// libs
import _ from 'lodash';

// components
import MultiSelectRow from '../table/multi-select-row';

type Props = {
  giftCard: Object,
  columns: Columns,
  params: Object,
};

const GiftCardTransactionRow = (props: Props) => {
  const { giftCard, columns, params } = props;

  const setCellContents = (giftCard: Object, field: string) => {
    const r = _.get(giftCard, field, null);

    if (field == 'debit') return -r;
    else if (field == 'orderPayment') return _.get(r, 'cordReferenceNumber');

    return r;
  };

  return (
    <MultiSelectRow
      columns={columns}
      row={giftCard}
      setCellContents={setCellContents}
      params={params}
    />
  );
};

export default GiftCardTransactionRow;
