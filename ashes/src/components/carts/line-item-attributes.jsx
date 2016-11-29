/* @flow */

// libs
import React, { Component, Element } from 'react';

import Table from 'components/table/table';

type Props = {
  columns: Array<Object>;
  data: Array<Object>;
  spanNumber: number;
}

export default (props: Props) => (
  <tr className="line-item__more">
    <td className="line-item__more-content" colSpan={props.spanNumber}>
      <Table
        columns={props.columns}
        data={props.data}
      />
    </td>
  </tr>
);
