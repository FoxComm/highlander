// libs
import React, { Component } from 'react';
import _ from 'lodash';
import { assoc } from 'sprout-data';
import { autobind } from 'core-decorators';

// components
import MultiSelectTable from 'components/table/multi-select-table';
import EditableSkuRow from './skus/editable-sku-row';
import { setSkuAttribute } from 'paragons/product';

// styles
import s from './product-amazon.css';

export default class ProductAmazonVariants extends Component {

  @autobind
  renderRow(row, index, columns, params) {
    const key = row.feCode || row.code || row.id;

    return (
      <EditableSkuRow
        columns={columns}
        sku={row}
        index={index}
        params={params}
        key={key}
        updateFields={this._handleSetSkuProperties}
      />
    );
  }

  render() {
    const { product, onChange } = this.props;
    const { skus } = product;

    if (!skus.length) {
      return null;
    }

    const arr = [
      { field: 'sku', text: 'SKU' },
      { field: 'image', text: 'Image' },
      { field: 'inventory', text: 'Inventory' },
      { field: 'upc', text: 'UPC' },
    ];

    const onCheck = function(skuIds) {
      let nextProduct = this.props.product;

      _.each(product.skus, (sku, i) => {
        const checked = _.includes(skuIds, sku.id);

        nextProduct = assoc(nextProduct, ['skus', i, 'attributes', 'amazon'], { t: 'bool', v: checked });
      });

      onChange(nextProduct);
    };

    const checkedIds = product.skus
      .filter(sku => _.get(sku, 'attributes.amazon.v'))
      .map(sku => sku.id);

    return (
      <MultiSelectTable
        className={s.table}
        tbodyId="fct-sku-list"
        columns={arr}
        initialCheckedIds={checkedIds}
        dataTable={false}
        data={{ rows: skus, total: skus.length }}
        renderRow={this.renderRow}
        emptyMessage="emptyMessage"
        hasActionsColumn={false}
        onCheck={onCheck.bind(this)}
      />
    );
  }

  @autobind
  _handleSetSkuProperties(code: string, updateArray: Array<Array<any>>) {
    const { product, onChange } = this.props;

    if (product) {
      const nextProduct = _.reduce(updateArray, (p, [field, value]) => {
        return setSkuAttribute(p, code, field, value, 'string');
      }, product);

      onChange(nextProduct);
    }
  }
}
