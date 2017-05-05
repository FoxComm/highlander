
// libs
import _ from 'lodash';
import React from 'react';
import PropTypes from 'prop-types';

// components
import MultiSelectRow from '../table/multi-select-row';

const setCellContents = (giftCard, field) => {
  const r = _.get(giftCard, field, null);

  if (field == 'debit') return -r;
  else if (field == 'orderPayment') return _.get(r, 'cordReferenceNumber');

  return r;
};

const GiftCardTransactionRow = props => {
  const { giftCard, columns, params } = props;

  const key = `gc-transaction-${giftCard.code}`;

  return (
    <MultiSelectRow
      columns={columns}
      row={giftCard}
      setCellContents={setCellContents}
      params={params}
    />
  );
};

GiftCardTransactionRow.propTypes = {
  giftCard: PropTypes.object.isRequired,
  columns: PropTypes.array.isRequired,
  params: PropTypes.object.isRequired,
};

export default GiftCardTransactionRow;
