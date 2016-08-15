/**
 * @flow
 */

import React, { Component, Element } from 'react';
import { assoc } from 'sprout-data';
import { autobind } from 'core-decorators';
import _ from 'lodash';
import { connect } from 'react-redux';
import Api from 'lib/api';
import styles from './editable-sku-row.css';

import { FormField } from '../forms';
import CurrencyInput from '../forms/currency-input';
import MultiSelectRow from '../table/multi-select-row';
import LoadingInputWrapper from '../forms/loading-input-wrapper';

import { suggestSkus } from 'modules/products/details';
import type { Sku } from 'modules/skus/details';

type Column = {
  field: string,
  text: string,
};

type SearchViewSku = {
  price: string,
  code: string,
  id: number,
  context: string,
  image: string|null,
}

type Props = {
  columns: Array<Column>,
  sku: Sku,
  params: Object,
  skuContext: string,
  updateField: (code: string, field: string, value: string) => void,
  isFetchingSkus: boolean|null,
  suggestSkus: (context: string, code: string) => Promise,
  suggestedSkus: Array<SearchViewSku>,
};

type State = {
  sku: { [key:string]: string },
  isMenuVisible: boolean,
};

function mapStateToProps(state) {
  return {
    isFetchingSkus: _.get(state.asyncActions, 'products-suggestSkus.inProgress', null),
    suggestedSkus: _.get(state, 'products.details.suggestedSkus', []),
  };
}

function stop(event: SyntheticEvent) {
  event.stopPropagation();
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
    if (this.props.sku.code) {
      return this.props.sku.code;
    }

    return this.props.sku.feCode || 'new';
  }

  suggestSkus(text: string) {
    return this.props.suggestSkus(this.props.skuContext, text);
  }

  @autobind
  handleSelectSku(searchViewSku: SearchViewSku) {
    this.closeSkusMenu();

    this.updateSku({
      salePrice: searchViewSku.price,
      code: searchViewSku.code,
    });
    // @TODO: get retailPrice directly from `sku` variable when this issue will be resolved:
    // https://github.com/FoxComm/green-river/issues/135
    Api.get(`/skus/${this.props.skuContext}/${searchViewSku.code}`).then(sku => {
      const retailPrice = _.get(sku.attributes, 'retailPrice.v.value', 0);
      this.updateSku({
        retailPrice,
      });
    });
  }

  @autobind
  closeSkusMenu() {
    this.setState({
      isMenuVisible: false
    });
  }

  get menuEmptyContent(): Element {
    return (
      <li
        styleName="sku-item"
        className="_new"
        onMouseDown={ this.closeSkusMenu }
      >
        <div>New SKU</div>
        <strong>{this.state.sku.code}</strong>
      </li>
    );
  }

  get menuItemsContent(): Array<Element> {
    const items = this.props.suggestedSkus;

    return items.map(sku => {
      return (
        <li
          styleName="sku-item"
          onMouseDown={() => { this.handleSelectSku(sku); }}
          key={`item-${sku.id}`}
        >
          <strong>{sku.code}</strong>
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
      default:
        return null;
    }
  }

  @autobind
  handleUpdateCode({ target }: Object) {
    const { value } = target;
    this.updateSku({
      code: value,
    });
    this.suggestSkus(value);
  }

  updateSku(values: {[key: string]: any}) {
    this.setState({
      sku: Object.assign({}, this.state.sku, values),
    }, () => {
      _.each(values, (value: any, field: string) => {
        this.props.updateField(this.code, field, value);
      });
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

export default connect(mapStateToProps, { suggestSkus })(EditableSkuRow);
