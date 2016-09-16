/**
 * @flow
 */

// libs
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import _ from 'lodash';

// components
import EditableSkuRow from './editable-sku-row';
import MultiSelectTable from '../table/multi-select-table';

import type { Product } from 'paragons/product';
import type { Sku } from 'modules/skus/details';

type UpdateFn = (code: string, field: string, value: any) => void;

type Props = {
  fullProduct: ?Product,
  updateField: UpdateFn,
  updateFields: (code: string, toUpdate: Array<Array<any>>) => void,
};

export default class SkuList extends Component {
  props: Props;

  get tableColumns(): Array<Object> {
    const variants = _.get(this.props, ['fullProduct', 'variants'], []);
    const variantColumns = _.map(variants, variant => ({ field: variant.name, text: variant.name }));
    return [
      { field: 'sku', text: 'SKU' },
      { field: 'imageUrl', text: 'Image' },
      ...variantColumns,
      { field: 'retailPrice', text: 'Retail Price' },
      { field: 'salePrice', text: 'Sale Price' },
    ];
  }

  get skus(): Array<Sku> {
    if (this.props.fullProduct) {
      return this.props.fullProduct.skus;
    }

    return [];
  }

  get emptyContent(): Element {
    return (
      <div className="fc-content-box__empty-text">
        No SKUs.
      </div>
    );
  }

  get productContext(): string {
    if (this.props.fullProduct) {
      return this.props.fullProduct.context.name;
    }
    return 'default';
  }

  skuContent(skus: Array<Sku>): Element {
    const renderRow = (row, index, columns, params) => {
      const key = row.feCode || row.code || row.id;

      return (
        <EditableSkuRow
          skuContext={this.productContext}
          columns={columns}
          sku={row}
          params={params}
          updateField={this.props.updateField}
          updateFields={this.props.updateFields}
          key={key}
        />
      );
    };

    return (
      <div className="fc-sku-list">
        <MultiSelectTable
          columns={this.tableColumns}
          dataTable={false}
          data={{ rows: skus }}
          renderRow={renderRow}
          emptyMessage="This product does not have any SKUs."
          hasActionsColumn={false} />
      </div>
    );
  }

  render(): Element {
    console.log(this.props.fullProduct);
    return _.isEmpty(this.skus)
      ? this.emptyContent
      : this.skuContent(this.skus);
  }
}
