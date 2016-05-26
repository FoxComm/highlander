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

type UpdateFn = (code: string, field: string, value: any) => void;

type Props = {
  fullProduct: ?FullProduct,
  updateField: UpdateFn,
};

const tableColumns = [
  { field: 'sku', text: 'SKU' },
  { field: 'retailPrice', text: 'Retail Price' },
  { field: 'salePrice', text: 'Sale Price' },
];

export default class SkuList extends Component<void, Props, void> {
  props: Props;

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
      const code = row.code || `new-${index}`;
      const key = `sku-${code}`;

      return (
        <EditableSkuRow
          columns={columns}
          sku={row}
          params={params}
          updateField={this.props.updateField}
          key={key}
        />
      );
    };

    return (
      <div className="fc-sku-list">
        <MultiSelectTable
          columns={tableColumns}
          data={{ rows: skus }}
          renderRow={renderRow}
          emptyMessage="This product does not have any SKUs."
          hasActionsColumn={false} />
      </div>
    );
  }

  render(): Element {
    return _.isEmpty(this.illuminatedSkus)
      ? this.emptyContent
      : this.skuContent(this.illuminatedSkus);
  }
}
