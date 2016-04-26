/**
 * @flow
 */

import React, { PropTypes, Element } from 'react';

import _ from 'lodash';
import { transitionTo } from 'browserHistory';
import { activeStatus } from '../../paragons/common';

import RoundedPill from '../rounded-pill/rounded-pill';
import MultiSelectRow from '../table/multi-select-row';

type PromotionRowProps = {
  promotion: Object,
  columns: Array<string>,
  params: Object,
};

const setCellContents = (promotion: Object, field: string) => {
  switch (field) {
    case 'state':
      return <RoundedPill text={activeStatus(promotion)} />;
    default:
      return _.get(promotion, field);
  }
};

const PromotionRow = (props: PromotionRowProps) => {
  const { promotion, columns, params } = props;
  const key = `promotion-${promotion.id}`;
  const clickAction = () => {
    transitionTo('promotion-details', { promotionId: promotion.id });
  };

  return (
    <MultiSelectRow
      cellKeyPrefix={key}
      columns={columns}
      onClick={clickAction}
      row={promotion}
      setCellContents={setCellContents}
      params={params} />
  );
};

PromotionRow.propTypes = {
  promotion: PropTypes.object,
  columns: PropTypes.array,
  params: PropTypes.object,
};

export default PromotionRow;
