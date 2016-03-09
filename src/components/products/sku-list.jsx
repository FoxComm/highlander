/**
 * @flow
 */

// libs
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { getIlluminatedSkus } from '../../paragons/product';
import _ from 'lodash';

// components
import EditableSkuRow from './editable-sku-row';
import MultiSelectTable from '../table/multi-select-table';

import type { FullProduct } from '../../modules/products/details';
import type { IlluminatedSku } from '../../paragons/product';

type UpdateFn = (code: string, field: string, value: string) => void;

type Props = {
  fullProduct: ?FullProduct,
  updateField: UpdateFn,
};

export default class SkuList extends Component<void, Props, void> {
  static tableColumns = [
    { field: 'image', text: 'Image' },
    { field: 'price', text: 'Price' },
    { field: 'sku', text: 'SKU' },
    { field: 'upc', text: 'UPC' },
  ];

  get illuminatedSkus(): Array<IlluminatedSku> {
    return this.props.fullProduct
      ? getIlluminatedSkus(this.props.fullProduct)
      : [];
  }

  get emptyContent(): Element {
    return (
      <div className="fc-content-box__empty-text">
        No SKUs.
      </div>
    );
  }

  skuContent(skus: Array<IlluminatedSku>): Element {
    const renderRow = (row, index, columns, params) => {
      const key = `sku-${row.code}`;
      return (
        <EditableSkuRow
          columns={columns}
          isNew={false}
          sku={row}
          params={params}
          updateField={this.props.updateField} />
      );
    };

    return (
      <MultiSelectTable
        columns={SkuList.tableColumns}
        data={{ rows: this.illuminatedSkus }}
        renderRow={renderRow}
        emptyMessage="No SKUs motherfucker." />
    );
  }

  render(): Element {
    return _.isEmpty(this.illuminatedSkus)
      ? this.emptyContent
      : this.skuContent(this.illuminatedSkus);
  }
}
