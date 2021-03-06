/* @flow */

// libs
import React, { Element } from 'react';

import Table from 'components/table/table';

type Props = {
  columns: Array<Object>;
  data: Object;
  spanNumber: number;
};

export default (props: Props) => (
  <tr className="line-item__attributes">
    <td colSpan={props.spanNumber}>
      <Table
        tbodyId="fct-cart-line-items"
        columns={props.columns}
        data={props.data}
      />
    </td>
  </tr>
);
