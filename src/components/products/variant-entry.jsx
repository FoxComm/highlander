/**
 * @flow
 */

import React, { Component, Element } from 'react';
import _ from 'lodash';

import ContentBox from '../content-box/content-box';
import TableBody from '../table/body';
import VariantValueEntry from './variant-value-entry';

import type { Variant, VariantValue } from '../../modules/products/details';

type Props = {
  variant: ?Variant,
};

export default class VariantEntry extends Component<void, Props, void> {
  get content(): ?Element {
    const values = this.values;
    if (_.isEmpty(values)) {
      return (
        <div className="fc-content-box__empty-text">
          This variant does not have values applied.
        </div>
      );
    } else {
      const variantName = _.get(this.props, 'variant', '');
      const entries = _.map(this.values, (value, name) => {
        const key = `product-variant-${variantName}-${name}`;
        return <VariantValueEntry key={key} name={name} value={value} />;
      });

      return (
        <div className="fc-variant-entry">
          <table className="fc-table">
            <tbody className="fc-table-tbody">
              {entries}
            </tbody>
          </table>
        </div>
      );
    }
  }

  get values(): { [key:string]: VariantValue } {
    return _.get(this.props, 'variant.values', {});
  }

  render(): Element {
    let title = 'Empty';
    if (this.props.variant) {
      title = this.props.variant.name;
    }

    return (
      <ContentBox title={title} indentContent={false}>
        {this.content}
      </ContentBox>
    );
  }
}

