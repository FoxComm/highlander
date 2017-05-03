/* @flow */

import React, { Component } from 'react';
// libs
import { numberize } from 'lib/text-utils';
import { autobind } from 'core-decorators';
import _ from 'lodash';

// components
import modal from 'components/modal/wrapper';
import ContentBox from 'components/content-box/content-box';
import SaveCancel from 'components/core/save-cancel';
import TextInput from 'components/forms/text-input';

type Props = {
  entity: string,
  onCancel: () => void,
  count: number,
  onConfirm: (description: ?string) => Promise<*>,
};

type State = {
  value: string,
};
class BulkExportModal extends Component {
  props: Props;

  state: State = {
    value: '',
  };

  @autobind
  handleChange(value) {
    this.setState({ value });
  }

  @autobind
  handleKeyDown(event) {
    const { key } = event;
    if (key == 'Enter') {
      this.handleSave();
    }
  }

  @autobind
  handleSave() {
    const description = !_.isEmpty(this.state.value) ? this.state.value : null;
    this.props.onConfirm(description);
  }

  get footer() {
    const { onCancel } = this.props;
    return (
      <SaveCancel
        className="fc-modal-footer fc-bulk-export-modal-footer"
        cancelTabIndex="2"
        cancelText="Cancel"
        onCancel={onCancel}
        saveTabIndex="1"
        onSave={this.handleSave}
        saveText="Yes, Export"
      />
    );
  }

  get actionBlock() {
    return (
      <i onClick={this.props.onCancel} className="fc-btn-close icon-close" title="Close" />
    );
  }

  get label() {
    const { entity, count } = this.props;
    const entityForm = numberize(entity, count);

    return (
      <span>Are you sure you want to export <b>{count} {entityForm}</b>?</span>
    );
  }

  render() {
    return (
      <ContentBox
        title="Export Selected Orders"
        actionBlock={this.actionBlock}
        footer={this.footer}
        className="fc-bulk-action-modal"
      >
        <div className="fc-modal-body">{this.label}</div>
        <TextInput
          onChange={this.handleChange}
          onKeyDown={this.handleKeyDown}
          placeholder="Short description (optional)"
          value={this.state.value}
          className="fc-bulk-export-modal-description"
        />
      </ContentBox>
    );
  }
}

export default modal(BulkExportModal);