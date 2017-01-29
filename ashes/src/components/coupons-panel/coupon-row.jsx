
/* @flow */

import { get, noop } from 'lodash';
import React, { Element } from 'react';

import { DeleteButton } from 'components/common/buttons';
import MultiSelectRow from 'components/table/multi-select-row';

function setCellContentsFunctionFactory(onDelete: Function): Function {
  const setCellContents = (item: Object, field: string) => {
    switch (field) {
      case 'edit':
        return <DeleteButton onClick={onDelete} />;
      case 'code':
        return get(item, ['code'], '');
      default:
        return get(item, ['coupon', 'attributes', field, 'v'], '');
    }
  };
  return setCellContents;
}

type Props = {
  item: Object,
  columns: Array<any>,
  params?: any,
  onDelete?: Function,
};

const CouponRow = ({item, columns, params, onDelete = () => {}}: Props): Element => {
  const checkedParams = params || {checked: false, setChecked: noop };

  const setCellContents = setCellContentsFunctionFactory(onDelete);

  return (
    <MultiSelectRow
      columns={columns}
      row={item}
      setCellContents={setCellContents}
      params={checkedParams}
    />
  );
};

export default CouponRow;
