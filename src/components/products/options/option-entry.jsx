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
import type { Variant, VariantValue } from 'paragons/product';

type Props = {
  id: string,
  option: ?Variant,
  editOption: Function,
  deleteOption: Function,
  editValues: Function,
};

class OptionEntry extends Component {
  props: Props;

  state = {
    editValue: null,
  };

  get values(): { [key:string]: VariantValue } {
    return _.get(this.props, 'option.values', []);
  }

  get content() {
    const optionName = _.get(this.props, 'option', '');

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

  get emptyContent() {
    return (
      <div className="fc-content-box__empty-text">
        This option does not have values applied.
      </div>
    );
  }

  get titleBarActions():Element {
    return (
      <div className="fc-option-entry__actions">
        <a onClick={() => this.editValue('new')} styleName="action-icon"><i className="icon-add"/></a>
        <a onClick={() => this.props.editOption(this.props.id)} styleName="action-icon"><i className="icon-edit"/></a>
        <a onClick={() => this.props.deleteOption(this.props.id)} styleName="action-icon"><i className="icon-trash"/></a>
      </div>
    );
  }

  @autobind
  editValue(id, value) {
    let editValue = { id };

    if (value) {
      editValue.value = value;
    } else {
      editValue.value = {
        name: '',
        swatch: '',
      }
    }

    this.setState({editValue})
  };

  @autobind
  deleteValue(id) {
    const values = this.values;

    values.splice(id, 1);

    const option = assoc(this.props.option, 'values', values);
    this.props.confirmAction(option, this.props.id);
  }

  @autobind
  updateValue(value, id) {
    const values = this.values;

    if (id === 'new') {
      values.push(value);
    } else {
      values[id] = value;
    }

    const option = assoc(this.props.option, 'values', values);
    this.props.confirmAction(option, this.props.id);

    this.setState({ editValue: null })
  }

  @autobind
  cancelEdit() {
    this.setState({ editValue: null })
  }

  render(): Element {
    const values = this.values;
    const content = _.isEmpty(values) ? this.emptyContent : this.content;
    const valueDialog = (
      <ValueEditDialog
        value={this.state.editValue}
        cancelAction={this.cancelEdit}
        confirmAction={this.updateValue}
      />
    );

    return (
      <ContentBox
        title={this.props.option.name}
        actionBlock={this.titleBarActions}
        indentContent={false}
        className="fc-option-entry"
      >
        {content}
        {this.state.editValue && valueDialog}
      </ContentBox>
    );
  }
}

export default OptionEntry;
