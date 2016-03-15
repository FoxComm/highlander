/**
 * @flow
 */

import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import _ from 'lodash';

import ContentBox from '../content-box/content-box';
import VariantEntry from './variant-entry';

import type { Variant } from '../../modules/products/details';

type Props = {
  variants: { [key:string]: Variant },
};

type State = {
  newVariants: { [key:string]: Variant },
};

export default class VariantList extends Component<void, Props, State> {
  state: State;

  constructor(props: Props) {
    super(props);
    this.state = { newVariants: {} };
  }

  get actions(): Element {
    return (
      <a onClick={this.handleAddNewVariant}>
        <i className="icon-add" />
      </a>
    );
  }

  get emptyContent(): Element {
    return (
      <div className="fc-content-box__empty-text">
        This product does not have variants.
      </div>
    );
  }

  get variantList(): Array<Element> {
    return [
      ...this.renderVariants(this.props.variants),
      ...this.renderVariants(this.state.newVariants),
    ];
  }

  @autobind
  handleAddNewVariant() {
    const newVariant: Variant = {
      name: null,
      type: null,
      values: {},
    };

    const lastKey = _.findLastKey(this.state.newVariants);
    const key = lastKey ? parseInt(lastKey) + 1 : 0;

    this.setState({
      newVariants: {
        ...this.state.newVariants,
        [key]: newVariant,
      },
    });
  }

  @autobind
  handleSaveNewVariant(variant: Variant, listKey: string) {
    console.log(variant);
  }

  @autobind
  handleCancelNewVariant(listKey: string) {
    this.setState({
      newVariants: _.omit(this.state.newVariants, listKey),
    });
  }

  renderVariants(variants: { [key:string]: Variant }): Array<Element> {
    return _.map(variants, (value, key) => {
      const reactKey = `product-variant-${key}`;
      return (
        <VariantEntry
          key={reactKey}
          variant={value}
          listKey={key}
          onSave={this.handleSaveNewVariant}
          onCancel={this.handleCancelNewVariant} />
      );
    });
  }

  render(): Element {
    const { variants } = this.props;
    const content = _.isEmpty(variants) ? this.emptyContent : this.variantList;

    return (
      <ContentBox title="Variants" actionBlock={this.actions}>
        {content}
      </ContentBox>
    );
  }
}
