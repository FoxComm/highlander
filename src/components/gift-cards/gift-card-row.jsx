
import React, { PropTypes } from 'react';

import _ from 'lodash';
import { transitionTo } from '../../route-helpers';

import MultiSelectRow from '../table/multi-select-row';
import OriginType from '../common/origin-type';

const setCellContents = (giftCard, field) => {
  if (field === 'originType') {
    return <OriginType value={giftCard} />;
  }

  return _.get(giftCard, field);
};

const GiftCardRow = (props, context) => {
  const { giftCard, columns, params } = props;
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
      setCellContents={setCellContents}
      params={params} />
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
