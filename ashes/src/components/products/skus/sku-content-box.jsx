/**
 * @flow
 */

// libs
import React, { Component, Element, PropTypes } from 'react';
import { autobind } from 'core-decorators';
import _ from 'lodash';
import { assoc, dissoc } from 'sprout-data';

// components
import ContentBox from 'components/content-box/content-box';
import SkuList from './sku-list';
import ConfirmationDialog from 'components/modal/confirmation-dialog';
import { Checkbox } from 'components/checkbox/checkbox';

// helpers
import { availableVariantsValues, variantsWithMultipleOptions } from 'paragons/variants';

// styles
import styles from './sku-content-box.css';

// types
import type { Product, OptionValue } from 'paragons/product';
import type { Sku } from 'modules/skus/details';

type UpdateFn = (code: string, field: string, value: any) => void;

type Props = {
  fullProduct: Product,
  updateField: UpdateFn,
  onDeleteSku: (skuCode: string) => void,
  onAddNewVariants: (options: Array<Array<OptionValue>>) => void,
  updateFields: (code: string, toUpdate: Array<Array<any>>) => void,
  variants: Array<any>,
};

type State = {
  addDialogIsShown: boolean,
  selectedOptions: {[key: string]: Array<OptionValue>},
};

class SkuContentBox extends Component {
  props: Props;
  state: State = {
    addDialogIsShown: false,
    selectedOptions: {},
  };

  get actions(): ?Element<*> {
    if (_.isEmpty(this.props.variants)) {
      return null;
    }
    const availableVariants = availableVariantsValues(this.props.fullProduct);
    if (_.isEmpty(availableVariants)) {
      return null;
    }

    return (
      <a id="fct-add-sku-btn__skus-block" styleName="add-icon" onClick={this.addAction}>
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

  get addSkuDialog(): Element<*> {
    const availableVariants = availableVariantsValues(this.props.fullProduct);
    const { selectedOptions } = this.state;

    const list = _.map(availableVariants, (values: Array<OptionValue>, i) => {
      const key = this.getValuesKey(values);
      const name = values.map(value => value.name).join('-').toLowerCase();
      const checked = !!selectedOptions[key];
      const content = values.map(value => value.name).join(', ');

      return (
        <li key={`sku-${key}`}>
          <Checkbox
            id={`sku-option-${i}`}
            name={`${name}-option-chbox`}
            onChange={() => this.toggleAddedOption(values)}
            checked={checked}
          >
            {content}
          </Checkbox>
        </li>
      );
    });

    const body = (
      <div styleName="add-dialog">
        <div styleName="dialog-subtitle">Available options:</div>
        <ul styleName="dialog-items">
          {list}
        </ul>
      </div>
    );
    return (
      <ConfirmationDialog
        key="add-skus"
        isVisible={this.state.addDialogIsShown}
        header="Add SKUs"
        body={body}
        cancel="Cancel"
        confirm="Add"
        onCancel={() => this.closeAction()}
        confirmAction={() => this.addNewSkus()}
      />
    );
  }

  @autobind
  addNewSkus() {
    const newVariants = _.values(this.state.selectedOptions);
    this.setState({
      selectedOptions: {}
    }, () => {
      this.props.onAddNewVariants(newVariants);
      this.closeAction();
    });
  }

  getValuesKey(values: Array<OptionValue>): string {
    return values.map(x => x.name).join('\x08');
  }

  @autobind
  toggleAddedOption(values: Array<OptionValue>) {
    let { selectedOptions } = this.state;
    const key = this.getValuesKey(values);
    selectedOptions = key in selectedOptions
      ? dissoc(selectedOptions, key)
      : assoc(selectedOptions, key, values);

    this.setState({
      selectedOptions,
    });
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
    const { props } = this;
    return (
      <ContentBox title="SKUs" actionBlock={ this.actions }>
        <SkuList
          key="sku-list"
          fullProduct={props.fullProduct}
          updateField={props.updateField}
          updateFields={props.updateFields}
          onDeleteSku={props.onDeleteSku}
          skus={this.skus}
          variants={variantsWithMultipleOptions(props.variants)}
        />
        { this.addSkuDialog }
      </ContentBox>
    );
  }
}

export default SkuContentBox;
