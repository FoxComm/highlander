
/* @flow */

import { get, noop } from 'lodash';
import React, { Element } from 'react';
import { DeleteButton } from '../../common/buttons';

import MultiSelectRow from '../../table/multi-select-row';

function setCellContentsFunctionFactory(onDelete: Function): Function {
  const setCellContents = (item: Object, field: string) => {
    switch (field) {
      case 'edit':
        return <DeleteButton onClick={onDelete} />;
      case 'code':
        return get(item, ['code'], '');
      default:
        return get(item, ['coupon', 'attributes', field, 'v'], '');
    ;}
  };
  return setCellContents;
}

type Props = {
  item: Object,
  columns: Array<any>,
  params: any,
  onDelete: Function,
};

const CouponRow = (props: Props): Element => {
  const { item, columns, params, onDelete } = props;
  const checkedParams = params || {checked: false, setChecked: noop };

  const key = `order-coupon-row-${item.id}`;

  const setCellContents = setCellContentsFunctionFactory(onDelete);

  return (
    <MultiSelectRow
      cellKeyPrefix={key}
      columns={columns}
      row={item}
      setCellContents={setCellContents}
      params={checkedParams}
    />
  );
};

export default CouponRow;
