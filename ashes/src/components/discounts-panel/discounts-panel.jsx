
/* @flow */

import React, { Component } from 'react';
import _ from 'lodash';
import classNames from 'classnames';

import ContentBox from 'components/content-box/content-box';
import PanelHeader from 'components/panel-header/panel-header';
import DiscountRow from './discount-row';
import TableView from 'components/table/tableview';

import styles from './discounts-panel.css';

type Props = {
  promotion: Object,
};

const viewColumns = [
  {field: 'name', text: 'Name'},
  {field: 'storefrontName', text: 'Storefront Name'},
];

export default class DiscountsPanel extends Component {
  props: Props;

  get title() {
    return (
      <PanelHeader isOptional={true} isCart={true} status="success" text="Discounts" />
    );
  }

  get discounts(): Array<Object> {
    const { promotion } = this.props;
    return promotion ? [promotion] : [];
  }

  get viewContent() {
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

  renderRow(row: Object, index: number, isNew: bool) {
    return (
      <DiscountRow
        key={`discount-row-${row.id}`}
        item={row}
        columns={viewColumns}
      />
    );
  }

  render() {

    return (
      <ContentBox
        title={this.title}
        indentContent={false}
        viewContent={this.viewContent}
      />
    );
  }
};
