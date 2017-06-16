/* @flow */

// libs
import _ from 'lodash';
import React from 'react';

// components
import MultiSelectRow from '../../table/multi-select-row';
import OriginType from '../../common/origin-type';

type Props = {
  storeCreditTransaction: Object,
  columns: Columns,
  params: Object,
};

const StoreCreditTransactionRow = (props: Props) => {
  const { storeCreditTransaction, columns, params } = props;

  const setCellContents = (transaction: Object, field: string) => {
    const state = _.get(transaction, field);

    switch(field) {
      case 'debit':
        return (-1) * _.get(transaction, field, null);
      case 'transaction':
        return (
          <OriginType value={transaction} />
        );
      case 'state':
        return state.charAt(0).toUpperCase() + state.slice(1);
      default:
        return _.get(transaction, field, null);
    }
  };

  return (
    <MultiSelectRow
      columns={columns}
      row={storeCreditTransaction}
      setCellContents={setCellContents}
      params={params} />
  );
};

export default StoreCreditTransactionRow;
