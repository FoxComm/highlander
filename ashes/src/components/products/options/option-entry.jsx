/**
 * @flow
 */

// libs
import _ from 'lodash';
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { assoc } from 'sprout-data';

// components
import ContentBox from 'components/content-box/content-box';
import ValueEntry from './value-entry';
import ValueEditDialog from './value-edit-dialog';

// styles
import styles from './option-list.css';

// types
import type { Option, OptionValue } from 'paragons/product';

type Props = {
  id: string,
  option: ?Option,
  editOption: Function,
  deleteOption: Function,
  confirmAction: (id: string|number, option: Option, deletingValue: ?OptionValue) => void,
};

type Value = {
  id: string|number,
  value: OptionValue,
};


type State = {
  editValue: ?Value,
};

class OptionEntry extends Component {
  props: Props;

  state: State = {
    editValue: null,
  };

  get values(): Array<OptionValue> {
    return _.get(this.props, 'option.values', []);
  }

  get content(): Element {
    const optionName = _.get(this.props, 'option.attributes.name.v', '');

    const entries = _.map(this.values, (value, key) => {
      return (
        <ValueEntry
          key={`product-option-${optionName}-${key}`}
          id={key}
          value={value}
          deleteValue={this.deleteValue}
          editValue={this.editValue}
        />
      );
    });

    return (
      <table className="fc-table">
        <tbody>
        {entries}
        </tbody>
      </table>
    );
  }

  get emptyContent(): Element {
    return (
      <div className="fc-content-box__empty-text">
        This option does not have values applied.
      </div>
    );
  }

  get titleBarActions(): Element {
    return (
      <div className="fc-option-entry__actions">
        <a onClick={() => this.editValue('new')} styleName="action-icon"><i className="icon-add"/></a>
        <a onClick={() => this.props.editOption(this.props.id)} styleName="action-icon"><i className="icon-edit"/></a>
        <a
          onClick={() => this.props.deleteOption(this.props.id)}
          styleName="action-icon">
          <i className="icon-trash"/>
        </a>
      </div>
    );
  }

  @autobind
  editValue(id: string|number, value?: OptionValue = {name: '', swatch: '', image: '', skuCodes: []}): void {
    const editValue = { id, value };

    this.setState({editValue});
  };

  @autobind
  deleteValue(id: number): void {
    const values = this.values;

    const newValues = values.slice();
    newValues.splice(id, 1);

    const option = assoc(this.props.option, 'values', newValues);
    this.props.confirmAction(this.props.id, option, values[id]);
  }

  @autobind
  updateValue(value: OptionValue, id: any): void {
    const values = this.values;
    const newValues = id == 'new' ? [...values, value] : assoc(values, id, value);

    const option = assoc(this.props.option, 'values', newValues);
    this.props.confirmAction(this.props.id, option, void 0);

    this.setState({ editValue: null });
  }

  @autobind
  cancelEdit(): void {
    this.setState({ editValue: null });
  }

  get valueDialog() {
    if (!this.state.editValue) return;

    return(
      <ValueEditDialog
        value={this.state.editValue}
        cancelAction={this.cancelEdit}
        confirmAction={this.updateValue}
      />
    );
  }

  render(): Element {
    const values = this.values;
    const name = _.get(this.props, 'option.attributes.name.v');
    const content = _.isEmpty(values) ? this.emptyContent : this.content;

    return (
      <ContentBox
        title={name}
        actionBlock={this.titleBarActions}
        indentContent={false}
        className="fc-option-entry">
        {content}
        {this.valueDialog}
      </ContentBox>
    );
  }
}

export default OptionEntry;
