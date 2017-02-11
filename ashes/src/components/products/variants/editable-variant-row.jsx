// @flow

import React, { Component, Element, PropTypes } from 'react';
import { autobind } from 'core-decorators';
import _ from 'lodash';
import { connect } from 'react-redux';
import { makeLocalStore, addAsyncReducer } from '@foxcomm/wings';
import styles from './editable-variant-row.css';

import { FormField } from 'components/forms';
import CurrencyInput from 'components/forms/currency-input';
import MultiSelectRow from 'components/table/multi-select-row';
import LoadingInputWrapper from 'components/forms/loading-input-wrapper';
import { Link } from 'components/link';
import StateDropdown from 'components/object-page/state-dropdown';

import reducer, { suggestSkus } from 'modules/skus/suggest';
import type { ProductVariant } from 'modules/product-variants/details';
import { productVariantCode } from 'paragons/product';

import type { StateChangeEvent } from 'components/object-page/state-dropdown';

type Column = {
  field: string,
  text: string,
};

type Props = {
  columns: Array<Column>,
  productVariant: ProductVariant,
  index: number,
  params: Object,
  updateField: (id: string, field: string, value: any) => void,
  updateFields: (id: string, toUpdate: Array<Array<any>>) => void,
  onDeleteClick: (id: string) => void,
  isFetchingSkus: boolean,
  skuOptionsMap: Object,
  suggestSkus: (code: string) => Promise,
  suggestedSkus: Array<TSearchViewSku>,
  options: Array<any>,
};

type State = {
  variant: { [key:string]: string },
  isMenuVisible: boolean,
  codeError?: Object,
};

function mapLocalStateToProps(state) {
  return {
    isFetchingSkus: _.get(state.asyncActions, 'suggestSkus.inProgress', false),
    suggestedSkus: _.get(state, 'skus', []),
  };
}

function stop(event: SyntheticEvent) {
  event.stopPropagation();
}

function pickProductVariantAttrs(sku: TSearchViewSku) {
  return {
    code: sku.sku,
  };
}

class EditableVariantRow extends Component {
  props: Props;

  state: State = {
    variant: {},
    isMenuVisible: false,
  };

  static contextTypes = {
    validationDispatcher: PropTypes.object,
  };

  toggleBindToDispatcher(bind) {
    const { validationDispatcher } = this.context;
    if (validationDispatcher) {
      const toggleBind = bind ? validationDispatcher.on : validationDispatcher.removeListener;

      toggleBind.call(validationDispatcher, 'errors', this.handleValidationErrors);
    }
  }

  componentDidMount() {
    this.toggleBindToDispatcher(true);
  }

  componentWillUnmount() {
    this.toggleBindToDispatcher(false);
  }

  @autobind
  handleValidationErrors(event) {
    const codeError = _.find(event.errors, error => {
      return error.path == `variants.${this.props.index}.attributes.code`;
    });
    if (codeError) {
      event.preventSave();
    }

    this.setState({
      codeError,
    });
  }

  componentWillReceiveProps(nextProps: Props) {
    if (this.props.isFetchingSkus && !nextProps.isFetchingSkus) {
      this.setState({
        isMenuVisible: true,
      });
    }
  }

  updateSkuFromSuggest() {
    const { props } = this;

    if (!_.isEmpty(props.suggestedSkus)) {
      const matchedSku = _.find(props.suggestedSkus, {code: this.skuCodeValue.toUpperCase()});
      if (matchedSku) {
        this.updateAttrsBySku(matchedSku);
      }
    }
  }

  @autobind
  priceCell(productVariant: ProductVariant, field: string): Element {
    const value = _.get(this.state.variant, [field, 'value'])
      || _.get(productVariant, ['attributes', field, 'v', 'value']);
    const currency = _.get(productVariant, ['attributes', field, 'v', 'currency'], 'USD');
    const onChange = (value) => this.handleUpdatePrice(field, value, currency);
    return (
      <div styleName="price">
        <CurrencyInput value={value} currency={currency} onChange={onChange} />
      </div>
    );
  }

  @autobind
  upcCell(productVariant: ProductVariant): Element {
    const value = this.state.variant.upc || _.get(productVariant, 'attributes.upc.v');
    return (
      <FormField>
        <input type="text" value={value} onChange={this.handleUpdateUpc} />
      </FormField>
    );
  }

  get variantId(): string {
    return productVariantCode(this.props.productVariant);
  }

  suggestSkus(text: string): Promise|void {
    return this.props.suggestSkus(text);
  }

  updateAttrsBySku(sku: TSearchViewSku) {
    this.updateVariant(pickProductVariantAttrs(sku));
  }

  @autobind
  handleSelectSku(sku: TSearchViewSku) {
    this.closeVariantsMenu(
      () => this.updateAttrsBySku(sku)
    );
  }

  closeVariantsMenu(callback: Function = _.noop) {
    this.setState({
      isMenuVisible: false
    }, callback);
  }

  get menuEmptyContent(): Element {
    return (
      <li
        id="create-new-sku-item"
        styleName="variant-row"
        className="_new"
        onMouseDown={() => this.closeVariantsMenu() }>
        <div>New SKU</div>
        <strong>{this.state.variant.code}</strong>
      </li>
    );
  }

  get menuItemsContent(): Array<Element> {
    const items = this.props.suggestedSkus;

    return items.map((sku: TSearchViewSku) => {
      return (
        <li
          id={`search-view-${sku.sku}`}
          styleName="variant-row"
          onMouseDown={() => { this.handleSelectSku(sku); }}
          key={`row-${sku.sku}`}>
          <strong>{sku.sku}</strong>
        </li>
      );
    });
  }

  get skusMenu(): Element {
    const content = _.isEmpty(this.props.suggestedSkus) ? this.menuEmptyContent : this.menuItemsContent;
    const openMenu =
      this.state.isMenuVisible && this.skuCodeValue.length > 0 && !this.props.isFetchingSkus;

    const className = openMenu ? '_visible' : void 0;

    return (
      <ul styleName="variants-menu" className={className} onMouseEnter={stop}>
        {content}
      </ul>
    );
  }

  get skuCodeValue(): string {
    const code = _.get(this.props, 'productVariant.attributes.code.v');
    return this.state.variant.code || code || '';
  }

  skuCell(productVariant: ProductVariant): Element {
    const code = _.get(productVariant, 'attributes.code.v');
    if (productVariant.skuId && productVariant.id) {
      return (
        <Link to="sku-details" styleName="sku-link" params={{skuId: productVariant.skuId}}>
          {code}
        </Link>
      );
    }
    const { codeError } = this.state;
    const error = codeError ? `SKU Code violates constraint: ${codeError.keyword}` : void 0;
    return (
      <div styleName="variant-cell">
        <FormField error={error} scrollToErrors>
          <LoadingInputWrapper inProgress={this.props.isFetchingSkus}>
            <input
              className="fc-text-input"
              type="text"
              value={this.skuCodeValue}
              onChange={this.handleUpdateCode}
              placeholder="SKU"
            />
          </LoadingInputWrapper>
        </FormField>
        {this.skusMenu}
      </div>
    );
  }

  imageCell(productVariant: ProductVariant): Element {
    const imageObject = _.get(productVariant, ['albums', 0, 'images', 0]);

    if (imageObject) {
      return (
        <div styleName="image-cell">
          <img {...imageObject} styleName="cell-thumbnail" />
        </div>
      );
    }

    return (
      <span styleName="no-image-text">No image.</span>
    );
  }

  optionCell(field: any, productVariant: ProductVariant): ?Element {
    if (field.indexOf('variant') < 0) {
      return null;
    }

    const idx = parseInt(field);
    const mapping = this.props.skuOptionsMap;

    const option = _.get(this.props.options, idx, {});
    const optionName = _.get(option, 'attributes.name.v');
    const variantCode = productVariantCode(productVariant);

    const optionValue = _.get(mapping, [variantCode, optionName]);

    return (
      <div styleName="variant-value">{optionValue}</div>
    );
  }

  @autobind
  handleStateChange(event: StateChangeEvent) {
    this.updateVariant({
      activeFrom: event.activeFrom,
      activeTo: event.activeTo,
    });
  }

  deleteIcon(productVariant: ProductVariant): ?Element {
    const variantCode = productVariantCode(productVariant);
    const skuValue = this.skuCodeValue;

    if (!_.isEmpty(this.props.options) || skuValue) {
      return (
        <i
          className="icon-trash"
          styleName="delete-icon"
          onClick={() => this.props.onDeleteClick(variantCode)}
        />
      );
    }
  }

  stateCell(productVariant: ProductVariant): Element {
    return (
      <div styleName="state-cell">
        <StateDropdown
          attributes={productVariant.attributes}
          onChange={this.handleStateChange}
        />
        {this.deleteIcon(productVariant)}
      </div>
    );
  }

  @autobind
  setCellContents(productVariant: ProductVariant, field: string): any {
    switch(field) {
      case 'sku':
        return this.skuCell(productVariant);
      case 'retailPrice':
      case 'salePrice':
        return this.priceCell(productVariant, field);
      case 'upc':
        return this.upcCell(productVariant);
      case 'image':
        return this.imageCell(productVariant);
      case 'state':
        return this.stateCell(productVariant);
      default:
        return this.optionCell(field, productVariant);
    }
  }

  @autobind
  handleUpdateCode({ target }: Object) {
    const { value } = target;
    this.updateVariant({
      code: value,
    });
    const promise = this.suggestSkus(value);
    if (promise) {
      promise.then(() => {
        this.updateSkuFromSuggest();
      });
    }
  }

  updateVariant(values: {[key: string]: any}) {
    this.setState({
      variant: Object.assign({}, this.state.variant, values),
    }, () => {
      const toUpdate = _.map(values, (value: any, field: string) => {
        return [field, value];
      });
      this.props.updateFields(this.variantId, toUpdate);
    });
  }

  @autobind
  handleUpdatePrice(field: string, value: string, currency: string) {
    this.updateVariant({
      [field]: {
        currency,
        value: Number(value),
      },
    });
  }

  @autobind
  handleUpdateUpc({target}: Object) {
    const value = target.value;

    this.updateVariant({
      upc: value,
    });
  }

  render(): Element {
    const { columns, productVariant, params } = this.props;

    return (
      <MultiSelectRow
        columns={columns}
        row={productVariant}
        params={params}
        setCellContents={this.setCellContents}
      />
    );
  }
}

export default _.flowRight(
  makeLocalStore(addAsyncReducer(reducer)),
  connect(mapLocalStateToProps, { suggestSkus })
)(EditableVariantRow);
