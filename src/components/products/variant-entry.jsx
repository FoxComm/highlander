/**
 * @flow
 */

import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import _ from 'lodash';

import ContentBox from '../content-box/content-box';
import TextInput from '../forms/text-input';
import VariantValueEntry from './variant-value-entry';

import type { Variant, VariantValue } from '../../modules/products/details';

type Props = {
  listKey: string,
  onCancel: (key: string) => void,
  onSave: (variant: Variant, listKey: string) => void,
  variant: ?Variant,
};

type State = {
  name: string,
  type: string,
};

export default class VariantEntry extends Component<void, Props, State> {
  props: Props;
  state: State;

  constructor(props: Props) {
    super(props);
    this.state = {
      name: '',
      type: '',
    };
  }

  get isNew(): bool {
    const name = _.get(this.props, 'variant.name');
    const type = _.get(this.props, 'variant.type');

    return !(name && type);
  }

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

    if (name && type) {
      return (
        <div className="fc-variant-entry__title-bar">
          <div className="fc-variant-entry__name">{name}</div>
          <div className="fc-variant-entry__type">{type}</div>
        </div>
      );
    } else {
      return (
        <div className="fc-variant-entry__title-bar">
          <TextInput
            className="fc-variant-entry__input"
            onChange={this.handleNameUpdate}
            placeholder="Variant Name"
            value={this.state.name} />
          <TextInput
            className="fc-variant-entry__input"
            onChange={this.handleTypeUpdate}
            placeholder="Variant Type"
            value={this.state.type} />
        </div>
      );
    }
  }

  get titleBarActions(): Element {
    if (this.isNew) {
      return (
        <div className="fc-variant-entry__title-bar-actions">
          <a onClick={this.handleCancel}><i className="icon-close" /></a>
          <a onClick={this.handleSave}><i className="icon-check" /></a>
        </div>
      );
    } else {
      return (
        <div className="fc-variant-entry__title-bar-actions">
          <a onClick={_.noop}><i className="icon-add" /></a>
          <a onClick={_.noop}><i className="icon-edit" /></a>
          <a onClick={_.noop}><i className="icon-trash" /></a>
        </div>
      );
    }
  }

  get values(): { [key:string]: VariantValue } {
    return _.get(this.props, 'variant.values', {});
  }

  @autobind
  handleNameUpdate(name: string) {
    this.setState({ name });
  }

  @autobind
  handleTypeUpdate(type: string) {
    this.setState({ type });
  }

  @autobind
  handleSave() {
    if (this.state.name.length > 0 && this.state.type.length > 0) {
      const variant: Variant = {
        ...this.props.variant,
        name: this.state.name,
        type: this.state.type,
      };

      this.props.onSave(variant, this.props.listKey);
    }
  }

  @autobind
  handleCancel() {
    this.props.onCancel(this.props.listKey);
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

