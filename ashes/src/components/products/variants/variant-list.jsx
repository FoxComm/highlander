// @flow

// libs
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import _ from 'lodash';
import styles from './variant-list.css';

// components
import EditableSkuRow from './editable-variant-row';
import TableView from 'components/table/tableview';
import ConfirmationDialog from 'components/modal/confirmation-dialog';

import { mapVariantsToOptions } from 'paragons/variants';

import type { Product } from 'paragons/product';
import type { ProductVariant } from 'modules/product-variants/details';


type Props = {
  fullProduct: Product,
  variants: Array<any>,
  updateField: (code: string, field: string, value: any) => void,
  updateFields: (code: string, toUpdate: Array<Array<any>>) => void,
  options: Array<any>,
  onDeleteVariant: (skuCode: string) => void,
};

type State = {
  isDeleteConfirmationVisible: boolean,
  skuCode: ?string,
  skuOptionsMap: Object,
};

export default class VariantList extends Component {
  props: Props;
  state: State = {
    isDeleteConfirmationVisible: false,
    skuCode: null,
    skuOptionsMap: mapVariantsToOptions(this.props.options),
  };

  componentWillReceiveProps(nextProps: Props) {
    if (this.props.options != nextProps.options || this.props.fullProduct.variants !== nextProps.fullProduct.variants) {
      const skuOptionsMap = mapVariantsToOptions(nextProps.options);
      this.setState({skuOptionsMap});
    }
  }

  get tableColumns(): Array<Object> {
    const { options } = this.props;
    const optionColumns = _.map(options, (option, idx) => {
      return {
        field: `${idx}_variant`,
        text: _.get(option, 'attributes.name.v'),
      };
    });

    return [
      { field: 'image', text: 'Image', type: 'image' },
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
    this.setState({ isDeleteConfirmationVisible: false, skuCode: null });
  }

  @autobind
  showDeleteConfirmation(skuCode: string): void {
    this.setState({ isDeleteConfirmationVisible: true, skuCode });
  }

  deleteVariant(): void {
    const { skuCode } = this.state;
    if (skuCode) {
      this.props.onDeleteVariant(skuCode);
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
        confirmAction={() => this.deleteVariant()}
      />
    );
  }

  skuContent(skus: Array<ProductVariant>): Element {
    const renderRow = (row, index) => {
      const key = row.feCode || row.code || row.id;

      return (
        <EditableSkuRow
          skuContext={this.productContext}
          columns={this.tableColumns}
          productVariant={row}
          index={index}
          params={{}}
          options={this.props.options}
          skuOptionsMap={this.state.skuOptionsMap}
          updateField={this.props.updateField}
          updateFields={this.props.updateFields}
          onDeleteClick={this.showDeleteConfirmation}
          key={key}
        />
      );
    };

    return (
      <div className="fc-sku-list">
        <TableView
          tbodyId="sku-list"
          styleName="sku-list"
          columns={this.tableColumns}
          dataTable={false}
          data={{ rows: skus }}
          renderRow={renderRow}
          emptyMessage="This product does not have any SKUs."
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
