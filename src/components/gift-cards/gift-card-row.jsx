import React, { PropTypes } from 'react';

import _ from 'lodash';
import { transitionTo } from '../../route-helpers';

import MultiSelectRow from '../table/multi-select-row';

const setCellContents = (giftCard, field) => {
  switch (field) {
    case 'id':
    case 'code':
    case 'originType':
    case 'originalBalance':
    case 'availableBalance':
    case 'currentBalance':
    case 'state':
    case 'createdAt':
      return _.get(giftCard, field);
  }
};

const GiftCardRow = (props, context) => {
  const { giftCard, columns } = props;
  const key = `gift-card-${giftCard.id}`;
  const clickAction = () => {
    transitionTo(context.history, 'giftcard', { giftCard: giftCard.code });
  };

  return (
    <MultiSelectRow
      cellKeyPrefix={key}
      columns={columns}
      onClick={clickAction}
      row={giftCard}
      setCellContents={setCellContents} />
  );
};

GiftCardRow.propTypes = {
  giftCard: PropTypes.object.isRequired,
  columns: PropTypes.array
};

GiftCardRow.contextTypes = {
  history: PropTypes.object.isRequired
};

export default GiftCardRow;
