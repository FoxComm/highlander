/**
 * @flow
 */

// libs
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import _ from 'lodash';
import styles from './sku-list.css';

// components
import EditableSkuRow from './editable-sku-row';
import MultiSelectTable from 'components/table/multi-select-table';
import ConfirmationDialog from 'components/modal/confirmation-dialog';

import { mapVariantsToOptions } from 'paragons/variants';

import type { Product } from 'paragons/product';
import type { ProductVariant } from 'modules/product-variants/details';

type UpdateFn = (code: string, field: string, value: any) => void;

type Props = {
  fullProduct: Product,
  variants: Array<any>,
  updateField: UpdateFn,
  updateFields: (code: string, toUpdate: Array<Array<any>>) => void,
  options: Array<any>,
  onDeleteSku: (skuCode: string) => void,
};

type State = {
  isDeleteConfirmationVisible: boolean,
  skuId: ?string,
  variantsSkusIndex: Object,
};

export default class SkuList extends Component {
  props: Props;
  state: State = {
    isDeleteConfirmationVisible: false,
    skuId: null,
    variantsSkusIndex: mapVariantsToOptions(this.props.options),
  };

  componentWillReceiveProps(nextProps: Props) {
    if (this.props.options != nextProps.options || this.props.fullProduct.variants !== nextProps.fullProduct.variants) {
      const variantsSkusIndex = mapVariantsToOptions(nextProps.options);
      this.setState({variantsSkusIndex});
    }
  }

  tableColumns(): Array<Object> {
    const { options } = this.props;
    const optionColumns = _.map(options, (option, idx) => {
      return {
        field: `${idx}_variant`,
        text: _.get(option, 'attributes.name.v'),
      };
    });

    return [
      ...optionColumns,
      { field: 'sku', text: 'SKU' },
      { field: 'retailPrice', text: 'Retail Price' },
      { field: 'salePrice', text: 'Sale Price' },
      { field: 'actions', text: '' },
    ];
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

  get hasOptions(): boolean {
    return !_.isEmpty(this.props.options);
  }

  @autobind
  closeDeleteConfirmation(): void {
    this.setState({ isDeleteConfirmationVisible: false, skuId: null });
  }

  @autobind
  showDeleteConfirmation(skuId: string): void {
    this.setState({ isDeleteConfirmationVisible: true, skuId });
  }

  @autobind
  deleteSku(): void {
    //ToDo: call something to delete SKU from product and variant
    const { skuId } = this.state;
    if (skuId) {
      this.props.onDeleteSku(skuId);
    }

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
        onCancel={() => this.closeDeleteConfirmation()}
        confirmAction={() => this.deleteSku()}
      />
    );
  }

  skuContent(skus: Array<ProductVariant>): Element {
    const renderRow = (row, index, columns, params) => {
      const key = row.feCode || row.code || row.id;

      return (
        <EditableSkuRow
          skuContext={this.productContext}
          columns={columns}
          sku={row}
          index={index}
          params={params}
          options={this.props.options}
          variantsSkusIndex={this.state.variantsSkusIndex}
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
          styleName="sku-list"
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
    return _.isEmpty(this.props.variants)
      ? this.emptyContent
      : this.skuContent(this.props.variants);
  }
}
