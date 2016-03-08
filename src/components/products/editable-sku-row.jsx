/**
 * @flow
 */

import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import _ from 'lodash';

import MultiSelectRow from '../table/multi-select-row';

import type { IlluminatedSku } from '../../paragons/product';

type Column = {
  field: string,
  text: string,
};

type Props = {
  columns: Array<Column>,
  sku: IlluminatedSku,
  params: Object,
};


export default class EditableSkuRow extends Component<void, Props, void> {
  @autobind
  setCellContents(sku: IlluminatedSku, field: string): any {
    switch(field) {
      case 'sku':
        return sku.code;
      case 'price':
        return _.get(sku, ['attributes', 'price', 'value', 'value']);
      default:
        return null;
    }
  }

  render(): Element {
    const { columns, sku, params } = this.props;

    const key = `pdp-sku-${sku.code}`;
    return (
      <MultiSelectRow
        cellKeyPrefix={key}
        columns={columns}
        onClick={_.noop}
        row={sku}
        params={params}
        setCellContents={this.setCellContents} />
    );
  }
}
