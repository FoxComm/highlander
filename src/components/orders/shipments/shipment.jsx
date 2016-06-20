/* @flow */

// libs
import _ from 'lodash';
import React, { Component, Element } from 'react';

// components
import ContentBox from '../../content-box/content-box';
import TableView from '../../table/tableview';
import PanelHeader from '../panel-header';

// styles
import styles from './shipment.css';

type Props = {
  index: number;
  total: number;
  details: Object;
};

export default class Shipment extends Component<void, Props, void> {
  props: Props;

  render(): Element {
    const { index, total } = this.props;
    const title = `Shipment ${index} of ${total}`;

    return (
      <ContentBox
        styleName="box"
        title={title}
        indentContent={false}
        viewContent={this.viewContent}
      />
    );
  }
}
