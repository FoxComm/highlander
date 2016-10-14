/**
 * @flow
 */

import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import _ from 'lodash';
import { connect } from 'react-redux';
import makeLocalStore from 'lib/make-local-store';
import styles from './editable-sku-row.css';

import { FormField } from 'components/forms';
import CurrencyInput from 'components/forms/currency-input';
import MultiSelectRow from 'components/table/multi-select-row';
import LoadingInputWrapper from 'components/forms/loading-input-wrapper';
import { DeleteButton } from 'components/common/buttons';

import reducer, { suggestSkus } from 'modules/skus/suggest';
import type { SuggestOptions } from 'modules/skus/suggest';
import type { Sku } from 'modules/skus/details';
import type { Sku as SearchViewSku } from 'modules/skus/list';
import { skuId } from 'paragons/product';

type Column = {
  field: string,
  text: string,
};

type Props = {
  columns: Array<Column>,
  sku: Sku,
  params: Object,
  skuContext: string,
  updateField: (code: string, field: string, value: string) => void,
  updateFields: (code: string, toUpdate: Array<Array<any>>) => void,
  onDeleteClick: (id: string) => void,
  isFetchingSkus: boolean|null,
  variantsSkusIndex: Object,
  suggestSkus: (code: string, context?: SuggestOptions) => Promise,
  suggestedSkus: Array<SearchViewSku>,
  variants: Array<any>,
};

type State = {
  sku: { [key:string]: string },
  isMenuVisible: boolean,
};

function mapLocalStateToProps(state) {
  return {
    isFetchingSkus: _.get(state, 'skus-suggest.inProgress', null),
    suggestedSkus: _.get(state, 'skus', []),
  };
}

function stop(event: SyntheticEvent) {
  event.stopPropagation();
}

function pickSkuAttrs(searchViewSku: SearchViewSku) {
  const sku = _.pick(searchViewSku, ['title', 'context', 'salePrice', 'retailPrice']);
  sku.code = searchViewSku.skuCode;
  return sku;
}

class EditableSkuRow extends Component {
  props: Props;

  state: State = {
    sku: {},
    isMenuVisible: false,
  };

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
        this.updateAttrsBySearchViewSku(matchedSku);
      }
    }
  }

  @autobind
  priceCell(sku: Sku, field: string): Element {
    const value = _.get(this.state.sku, field) || _.get(sku, ['attributes', field, 'v', 'value']);
    const onChange = (value) => this.handleUpdatePrice(field, value);
    return (
      <div className="fc-editable-sku-row__price">
        <CurrencyInput value={value} onChange={onChange} />
      </div>
    );
  }

  @autobind
  upcCell(sku: Sku): Element {
    const value = this.state.sku.upc || _.get(sku, 'attributes.upc.v');
    return (
      <FormField>
        <input type="text" value={value} onChange={this.handleUpdateUpc} />
      </FormField>
    );
  }

  get code(): string {
    return skuId(this.props.sku);
  }

  suggestSkus(text: string): Promise|void {
    return this.props.suggestSkus(text, {
      context: this.props.skuContext
    });
  }

  updateAttrsBySearchViewSku(searchViewSku: SearchViewSku) {
    this.updateSku(pickSkuAttrs(searchViewSku));
  }

  @autobind
  handleSelectSku(searchViewSku: SearchViewSku) {
    this.closeSkusMenu(
      () => this.updateAttrsBySearchViewSku(searchViewSku)
    );
  }

  closeSkusMenu(callback: Function = _.noop) {
    this.setState({
      isMenuVisible: false
    }, callback);
  }

  get menuEmptyContent(): Element {
    return (
      <li
        styleName="sku-item"
        className="_new"
        onMouseDown={() => this.closeSkusMenu() }>
        <div>New SKU</div>
        <strong>{this.state.sku.code}</strong>
      </li>
    );
  }

  get menuItemsContent(): Array<Element> {
    const items = this.props.suggestedSkus;

    return items.map((sku: SearchViewSku) => {
      return (
        <li
          styleName="sku-item"
          onMouseDown={() => { this.handleSelectSku(sku); }}
          key={`item-${sku.id}`}>
          <strong>{sku.skuCode}</strong>
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
      <ul styleName="skus-menu" className={className} onMouseEnter={stop}>
        {content}
      </ul>
    );
  }

  get skuCodeValue(): string {
    const code = _.get(this.props, 'sku.attributes.code.v');
    return this.state.sku.code || code || '';
  }

  skuCell(sku: Sku): Element {
    const code = _.get(this.props, 'sku.attributes.code.v');
    if (this.props.sku.feCode) {
      return (
        <div styleName="sku-cell">
          <FormField>
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

    return <div>{code}</div>;
  }

  imageCell(sku: Sku): Element {
    const imageObject = _.get(sku, ['albums', 0, 'images', 0]);

    if (!_.isEmpty(imageObject)) {
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

  variantCell(field: any, sku: Sku): ?Element {
    if (field.indexOf('variant') < 0) {
      return null;
    }

    const idx = parseInt(field);
    const mapping = this.props.variantsSkusIndex;

    const variant = _.get(this.props.variants, idx, {});
    const variantName = _.get(variant, 'attributes.name.v');
    const skuAttributeCode = _.get(sku, 'attributes.code.v');
    const skuCode = sku.feCode || skuAttributeCode;

    const variantValue = _.get(mapping, [skuCode, variantName]);

    return (
      <div styleName="variant-value">{variantValue}</div>
    );
  }

  actionsCell(sku: Sku): Element {
    const skuCode = sku.feCode || _.get(sku.attributes, 'code.v');
    return <DeleteButton onClick={() => this.props.onDeleteClick(skuCode)}/>;
  }

  @autobind
  setCellContents(sku: Sku, field: string): any {
    switch(field) {
      case 'sku':
        return this.skuCell(sku);
      case 'retailPrice':
      case 'salePrice':
        return this.priceCell(sku, field);
      case 'upc':
        return this.upcCell(sku);
      case 'image':
        return this.imageCell(sku);
      case 'actions':
        return this.actionsCell(sku);
      default:
        return this.variantCell(field, sku);
    }
  }

  @autobind
  handleUpdateCode({ target }: Object) {
    const { value } = target;
    this.updateSku({
      code: value,
    });
    const promise = this.suggestSkus(value);
    if (promise) {
      promise.then(() => {
        this.updateSkuFromSuggest();
      });
    }
  }

  updateSku(values: {[key: string]: any}) {
    this.setState({
      sku: Object.assign({}, this.state.sku, values),
    }, () => {
      const toUpdate = _.map(values, (value: any, field: string) => {
        return [field, value];
      });
      this.props.updateFields(this.code, toUpdate);
    });
  }

  @autobind
  handleUpdatePrice(field: string, value: string) {
    this.updateSku({
      [field]: value,
    });
  }

  @autobind
  handleUpdateUpc({target}: Object) {
    const value = target.value;

    this.updateSku({
      upc: value,
    });
  }

  render(): Element {
    const { columns, sku, params } = this.props;

    return (
      <MultiSelectRow
        columns={columns}
        row={sku}
        params={params}
        setCellContents={this.setCellContents} />
    );
  }
}

export default _.flowRight(
  makeLocalStore(reducer),
  connect(mapLocalStateToProps, { suggestSkus })
)(EditableSkuRow);
