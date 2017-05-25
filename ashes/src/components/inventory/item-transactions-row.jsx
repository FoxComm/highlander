/* @flow */

// libs
import _ from 'lodash';
import React from 'react';

// components
import MultiSelectRow from 'components/table/multi-select-row';

type Props = {
  transaction: Object,
  columns: Columns,
  params: Object,
};

const InventoryItemTransactionsRow = (props: Props) => {
  const { transaction, columns, params } = props;

  const setCellContents = (transaction: Object, field: string) => {
    return _.get(transaction, field);
  };

  return (
    <MultiSelectRow
      columns={columns}
      row={transaction}
      setCellContents={setCellContents}
      params={params}
    />
  );
};

export default InventoryItemTransactionsRow;
