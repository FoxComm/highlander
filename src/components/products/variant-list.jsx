/**
 * @flow
 */

import React, { Component, Element } from 'react';
import _ from 'lodash';

import ContentBox from '../content-box/content-box';
import VariantEntry from './variant-entry';

import type { Variant } from '../../modules/products/details';

type Props = {
  variants: { [key:string]: Variant },
};

export default class VariantList extends Component<void, Props, void> {
  get emptyContent(): Element {
    return (
      <div className="fc-content-box__empty-text">
        This product does not have variants.
      </div>
    );
  }

  get variantList(): Element {
    return _.map(this.props.variants, variant => {
      const key = `product-variant-${variant.name}`;
      return <VariantEntry key={key} variant={variant} />;
    });
  }

  render(): Element {
    const { variants } = this.props;
    const content = _.isEmpty(variants) ? this.emptyContent : this.variantList;

    return (
      <ContentBox title="Variants">
        {content}
      </ContentBox>
    );
  }
}
