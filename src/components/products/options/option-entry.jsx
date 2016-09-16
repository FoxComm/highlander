/**
 * @flow
 */

// libs
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import _ from 'lodash';

// components
import ContentBox from 'components/content-box/content-box';
import VariantValueEntry from './option-value-entry';

// styles
import styles from './option-list.css';

// types
import type { Variant, VariantValue } from 'paragons/product';

type Props = {
  id: string,
  variant: ?Variant,
  editOption: Function,
  deleteOption: Function,
};

class OptionEntry extends Component {
  props: Props;

  get content() {
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
            <tbody>
              {entries}
            </tbody>
          </table>
        </div>
      );
    }
  }

  get titleBar(): Element {
    const name = _.get(this.props, 'variant.name');
    const type = _.get(this.props, 'variant.type');

    return (
      <div className="fc-variant-entry__title-bar">
        <div className="fc-variant-entry__name">{name}</div>
        <div className="fc-variant-entry__type">{type}</div>
      </div>
    );
  }

  get titleBarActions():Element {
    return (
      <div className="fc-variant-entry__title-bar-actions">
        <a onClick={_.noop} styleName="add-icon"><i className="icon-add"/></a>
        <a onClick={() => this.props.editOption(this.props.id)} styleName="add-icon"><i className="icon-edit"/></a>
        <a onClick={() => this.props.deleteOption(this.props.id)} styleName="add-icon"><i className="icon-trash"/></a>
      </div>
    );
  }

  get values(): { [key:string]: VariantValue } {
    return _.get(this.props, 'variant.values', {});
  }

  render(): Element {
    return (
      <ContentBox
        title={this.titleBar}
        actionBlock={this.titleBarActions}
        indentContent={false}>
        {this.content}
      </ContentBox>
    );
  }
}

export default OptionEntry;
