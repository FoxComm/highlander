/* @flow */

// libs
import _ from 'lodash';
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';

// components
import ContentBox from 'components/content-box/content-box';
import TableView from 'components/table/tableview';
import ShipmentRow from './shipment-row';

// styles
import styles from './shipment.css';


// types
type Props = {
  index: number;
  total: number;
  details: Object;
};


const viewColumns = [
  {field: 'method', text: 'Shipping Method'},
  {field: 'state', text: 'Shipment State'},
  {field: 'items', text: 'Items'},
  {field: 'shipmentDate', text: 'Shipment Date'},
  {field: 'carrier', text: 'Carrier'},
  {field: 'estimatedArrival', text: 'Estimated Arrival'},
  {field: 'deliveredDate', text: 'Delivered On'},
  {field: 'trackingNumber', text: 'Tracking Number'},
];


export default class Shipment extends Component<void, Props, void> {
  props: Props;

  @autobind
  renderRow(row: Object, index: number): Element<*> {
    return (
      <ShipmentRow
        key={index}
        {...row}
      />
    );
  }

  get content() {
    return (
      <TableView
        columns={viewColumns}
        data={{rows: [this.props.details]}}
        wrapToTbody={false}
        renderRow={this.renderRow}
      />
    );
  }

  render() {
    const { index, total } = this.props;
    const title = `Shipment ${index} of ${total}`;

    return (
      <ContentBox
        styleName="box"
        title={title}
        indentContent={false}
        viewContent={this.content}
      />
    );
  }
}
