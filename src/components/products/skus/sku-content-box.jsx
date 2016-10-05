/**
 * @flow
 */

// libs
import React, { Component, Element, PropTypes } from 'react';
import { autobind } from 'core-decorators';
import _ from 'lodash';

// components
import ContentBox from 'components/content-box/content-box';
import SkuList from './sku-list';
import ConfirmationDialog from 'components/modal/confirmation-dialog';
import { Checkbox } from 'components/checkbox/checkbox';

// helpers
import { variantsWithMultipleOptions } from 'paragons/product';

// styles
import styles from './sku-content-box.css';

// types
import type { Product } from 'paragons/product';
import type { Sku } from 'modules/skus/details';

type UpdateFn = (code: string, field: string, value: any) => void;

type Props = {
  fullProduct: ?Product,
  updateField: UpdateFn,
  onDeleteSku: (skuCode: string) => void,
  updateFields: (code: string, toUpdate: Array<Array<any>>) => void,
  variants: Array<any>,
};

type State = {
  addDialogIsShown: boolean,
};

class SkuContentBox extends Component {
  props: Props;
  state: State = {
    addDialogIsShown: false,
  };

  get actions(): ?Element {
    if (_.isEmpty(this.props.variants)) {
      return null;
    }

    return (
      <a styleName="add-icon" onClick={this.addAction}>
        <i className="icon-add" />
      </a>
    );
  }

  get skus(): Array<Sku> {
    if (this.props.fullProduct) {
      return this.props.fullProduct.skus;
    }

    return [];
  }

  get addSkuDialog(): Element {
    const list = _.map(this.skus, (sku, key) => {
      let checked;

      return (
        <li>
          <Checkbox
            id={`sku-option-${key}`}
            onChange={this.toggleAddedSku}
            checked={checked}
          >
            {sku.attributes.code.v}
          </Checkbox>
        </li>
      );
    });

    const body = (
      <div styleName="add-dialog">
        <div styleName="dialog-subtitle">Available options:</div>
        <div styleName="dialog-items">
          <ul>
            {list}
          </ul>
        </div>
      </div>
    );
    return (
      <ConfirmationDialog
        isVisible={this.state.addDialogIsShown}
        header="Add SKUs"
        body={body}
        cancel="Cancel"
        confirm="Add"
        cancelAction={() => this.closeAction()}
        confirmAction={() => this.closeAction()}
      />
    );
  }

  @autobind
  toggleAddedSku() {

  }

  @autobind
  addAction() {
    this.setState({ addDialogIsShown: true });
  }

  @autobind
  closeAction() {
    this.setState({ addDialogIsShown: false });
  }

  render() {
    return (
      <ContentBox title="SKUs" actionBlock={ this.actions }>
        <SkuList
          fullProduct={this.props.fullProduct}
          updateField={this.props.updateField}
          updateFields={this.props.updateFields}
          onDeleteSku={this.props.onDeleteSku}
          skus={this.skus}
          variants={variantsWithMultipleOptions(this.props.variants)}
        />
        { this.addSkuDialog }
      </ContentBox>
    );
  }
}

export default SkuContentBox;
