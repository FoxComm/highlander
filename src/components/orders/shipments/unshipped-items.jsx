/* @flow */

// libs
import _ from 'lodash';
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';

// data
import { itemStateTitles, itemReasonsTitles } from 'paragons/shipment';

// components
import ContentBox from 'components/content-box/content-box';
import TableView from 'components/table/tableview';
import TableRow from 'components/table/row';
import TableCell from 'components/table/cell';
import Currency from 'components/common/currency';

// styles
import styles from './unshipped-items.css';

// types
import type { UnshippedLineItem } from 'paragons/shipment';

type Props = {
  items: Array<UnshippedLineItem>;
};


const viewColumns = [
  {field: 'imagePath', text: 'Image', type: 'image'},
  {field: 'name', text: 'Name'},
  {field: 'sku', text: 'SKU'},
  {field: 'price', text: 'Price', type: 'currency'},
  {field: 'quantity', text: 'Quantity'},
  {field: 'state', text: 'Item State'},
  {field: 'reason', text: 'Reason'},
];


export default class UnshippedItems extends Component<void, Props, void> {
  props: Props;

  @autobind
  renderRow(row: Object, index: number): Element {
    return (
      <TableRow key={index}>
        <TableCell>
          <img src={row.imagePath} />
        </TableCell>
        <TableCell>{row.name}</TableCell>
        <TableCell>{row.sku}</TableCell>
        <TableCell>
          <Currency value={row.price} />
        </TableCell>
        <TableCell>{row.quantity}</TableCell>
        <TableCell>{itemStateTitles[row.state]}</TableCell>
        <TableCell>{itemReasonsTitles[row.reason]}</TableCell>
      </TableRow>
    );
  }

  get content(): Element {
    const { items } = this.props;

    if (!items.length) {
      return <div styleName="empty">All Items have shipped!</div>;
    }

    return (
      <TableView
        columns={viewColumns}
        data={{rows: items}}
        wrapToTbody={false}
        renderRow={this.renderRow}
      />
    );
  }

  render(): Element {
    return (
      <ContentBox
        styleName="box"
        title="Unshipped Items"
        indentContent={false}
        viewContent={this.content}
      />
    );
  }
}
