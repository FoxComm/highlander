/**
 * @flow
 */

import React, { PropTypes, Element } from 'react';

import _ from 'lodash';
import { transitionTo } from 'browserHistory';

import { DateTime } from '../common/datetime';
import { Checkbox } from '../checkbox/checkbox';
import Currency from '../common/currency';
import Link from '../link/link';
import MultiSelectRow from '../table/multi-select-row';

type PromotionRowProps = {
  promotion: Object,
  columns: Array<string>,
  params: Object,
};

const setCellContents = (promotion: Object, field: string) => {
  return _.get(promotion, field);
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
