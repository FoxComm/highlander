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
  skus: Array<Any>,
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

  tableColumns(): Array<Object> {
    const { variants } = this.props;
    const variantColumns = [];
    _.each(variants, (variant, idx) => {
      const variantName = _.get(variant, 'attributes.name.v', variant.name);
      variantColumns.push({ field: `${idx}_variant`, text: variantName });
    });

    let columns = [
      { field: 'image', text: 'Image' },
      ...variantColumns,
      { field: 'sku', text: 'SKU' },
      { field: 'retailPrice', text: 'Retail Price' },
      { field: 'salePrice', text: 'Sale Price' },
    ];

    if (!_.isEmpty(variants) && this.props.skus.length > 1) {
      columns.push({ field: 'actions', text: '' });
    }

    return columns;
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
          columns={this.tableColumns()}
          dataTable={false}
          data={{ rows: skus }}
          renderRow={renderRow}
          emptyMessage="This product does not have any SKUs."
          hasActionsColumn={false}
        />
        { this.deleteDialog }
      </div>
    );
  }

  render(): Element {
    return _.isEmpty(this.props.skus)
      ? this.emptyContent
      : this.skuContent(this.props.skus);
  }
}
