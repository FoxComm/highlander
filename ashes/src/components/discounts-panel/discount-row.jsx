
/* @flow */

import { get, noop } from 'lodash';
import React, { Element} from 'react';

import MultiSelectRow from 'components/table/multi-select-row';

function setCellContents(item: Object, field: string): any {
  return get(item, ['attributes', field, 'v'], '');
}

type Props = {
  item: Object,
  columns: Array<any>,
  params?: any,
};

const DiscountRow = (props: Props): Element<*>=> {
  const { item, columns, params } = props;
  const checkedParams = params || {checked: false, setChecked: noop };

  return (
    <MultiSelectRow
      columns={columns}
      row={item}
      setCellContents={setCellContents}
      params={checkedParams}
    />
  );
};

export default DiscountRow;
