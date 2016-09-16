/**
 * @flow
 */

// libs
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import _ from 'lodash';

// components
import EditableSkuRow from './editable-sku-row';
import MultiSelectTable from 'components/table/multi-select-table';
import ConfirmationDialog from 'components/modal/confirmation-dialog';

import type { Product } from 'paragons/product';
import type { Sku } from 'modules/skus/details';

type UpdateFn = (code: string, field: string, value: any) => void;

type Props = {
  fullProduct: ?Product,
  updateField: UpdateFn,
  updateFields: (code: string, toUpdate: Array<Array<any>>) => void,
  variants: Array<any>,
};

type State = {
  isDeleteConfirmationVisible: boolean,
  skuId: ?string|?number,
};

export default class SkuList extends Component {
  props: Props;
  state: State = {
    isDeleteConfirmationVisible: false,
    skuId: null,
  };

  get tableColumns(): Array<Object> {
    const { variants } = this.props;
    const variantColumns = [];
    _.each(variants, (variant, idx) => {
      variantColumns.push({ field: `${idx}_variant`, text: variant.name });
    });

    return [
      { field: 'image', text: 'Image' },
      ...variantColumns,
      { field: 'sku', text: 'SKU' },
      { field: 'retailPrice', text: 'Retail Price' },
      { field: 'salePrice', text: 'Sale Price' },
      { field: 'actions', test: '' },
    ];
  }

  get availableVariants(): Array<Object> {
    const opts = _.map(this.props.variants, variant => variant.values);
    // magic of Cartesian product http://stackoverflow.com/questions/12303989/cartesian-product-of-multiple-arrays-in-javascript
    const variants = _.reduce(opts, function(a, b) {
        return _.flatten(_.map(a, function(x) {
            return _.map(b, function(y) {
                return x.concat([y]);
            });
        }), true);
    }, [ [] ]);
    return variants;
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

  get hasVariants(): boolean {
    return _.isEmpty(this.props.variants);
  }

  @autobind
  closeDeleteConfirmation(): void {
    this.setState({ isDeleteConfirmationVisible: false, skuId: null });
  }

  @autobind
  showDeleteConfirmation(skuId: string|number): void {
    this.setState({ isDeleteConfirmationVisible: true, skuId });
  }

  @autobind
  deleteSku(): void {
    //ToDo: call something to delete SKU from product and variant
    this.closeDeleteConfirmation();
  }

  get deleteDialog(): Element {
    const confirmation = (
      <span>
        Are you sure you want to remove this SKU from the product?
        This action will <i>not</i> archive the SKU.
      </span>
    );
    return (
      <ConfirmationDialog
        isVisible={this.state.isDeleteConfirmationVisible}
        header="Remove SKU from product?"
        body={confirmation}
        cancel="Cancel"
        confirm="Yes, Remove"
        cancelAction={() => this.closeDeleteConfirmation()}
        confirmAction={() => this.deleteSku()}
      />
    );
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
          variants={this.props.variants}
          updateField={this.props.updateField}
          updateFields={this.props.updateFields}
          onDeleteClick={this.showDeleteConfirmation}
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
        { this.deleteDialog }
      </div>
    );
  }

  render(): Element {
    this.availableVariants;
    return _.isEmpty(this.skus)
      ? this.emptyContent
      : this.skuContent(this.skus);
  }
}
