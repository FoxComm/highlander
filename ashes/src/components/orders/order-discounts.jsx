
/* @flow */

import React, { Component, Element } from 'react';
import _ from 'lodash';
import classNames from 'classnames';

import EditableContentBox from 'components/content-box/editable-content-box';
import ContentBox from 'components/content-box/content-box';
import PanelHeader from './panel-header';
import DiscountRow from './discounts/discount-row';
import TableView from 'components/table/tableview';

import styles from './styles/discounts.css';

type Props = {
  order: Object,
};

const viewColumns = [
  {field: 'name', text: 'Name'},
  {field: 'storefrontName', text: 'Storefront Name'},
];

export default class OrderDiscounts extends Component {
  props: Props;

  get title(): Element {
    return (
      <PanelHeader isOptional={true} text="Discounts" />
    );
  }

  get discounts(): Array<Object> {
    const promotion = _.get(this.props, 'order.promotion');

    if (!promotion) return [];

    return [promotion];
  }

  get viewContent(): Element {
    const discounts = this.discounts;
    if (_.isEmpty(discounts)) {
      return <div styleName="empty-message">No discounts applied.</div>;
    } else {
      return (
        <TableView
          columns={viewColumns}
          data={{rows: discounts}}
          renderRow={this.renderRow}
        />
      );
    }
  }

  renderRow(row: Object, index: number, isNew: bool): Element {
    return (
      <DiscountRow
        key={`order-discount-row-${row.id}`}
        item={row}
        columns={viewColumns}
      />
    );
  }

  render(): Element {
    return (
      <ContentBox
        title={this.title}
        indentContent={false}
        viewContent={this.viewContent}
      />
    );
  }
};
