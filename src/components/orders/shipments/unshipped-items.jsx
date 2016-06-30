/* @flow */

// libs
import _ from 'lodash';
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';

// components
import ContentBox from '../../content-box/content-box';
import TableView from '../../table/tableview';
import TableRow from '../../table/row';
import TableCell from '../../table/cell';
import Currency from '../../common/currency';

// styles
import styles from './unshipped-items.css';

// types
type Item = {
  imagePath: string;
  name: string;
  sku: string;
  price: number;
  quantity: number;
  state: string;
  reason: ?string;
};

type Props = {
  items: Array<Item>;
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

  get content(): Element {
    const { items } = this.props;

    if (!items.length) {
      return <span styleName="empty">All Items have shipped!</span>;
    }

    return (
      <TableView
        columns={viewColumns}
        data={{rows: items}}
        wrapToTbody={false}
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
