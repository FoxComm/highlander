/* @flow */

import React, { Component } from 'react';

// libs
import { numberize } from 'lib/text-utils';
import { autobind } from 'core-decorators';
import _ from 'lodash';
import classNames from 'classnames';

// components
import modal from 'components/modal/wrapper';
import ContentBox from 'components/content-box/content-box';
import SaveCancel from 'components/core/save-cancel';
import TextInput from 'components/forms/text-input';

import s from './bulk-export-modal.css';

type Props = {
  entity: string,
  onCancel: () => void,
  count: number,
  onConfirm: (description: ?string) => Promise<*>,
  title: string,
  inBulk: boolean,
};

type State = {
  value: string,
};
class BulkExportModal extends Component {
  props: Props;
  static defaultProps = {
    inBulk: false,
  };

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
    const className = classNames(
      'fc-modal-footer',
      s.exportModalFooter,
    );
    return (
      <SaveCancel
        className={className}
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
      <span>Are you sure you want to export <b>{count ? count : 'all'} {entityForm}</b>?</span>
    );
  }

  render() {
    const { inBulk, title } = this.props;
    const modalTitle = inBulk ? `Export All ${title}` : `Export Selected ${title}`;
    const fileName = title.toLowerCase().replace(' ', '_');

    return (
      <ContentBox
        title={modalTitle}
        actionBlock={this.actionBlock}
        footer={this.footer}
        className="fc-bulk-action-modal"
      >
        <div className="fc-modal-body">{this.label}</div>
        <div className={s.customTitle}>
          <span>{`${fileName}-`}</span>
          <TextInput
            onChange={this.handleChange}
            onKeyDown={this.handleKeyDown}
            placeholder="Custom Title (optional)"
            value={this.state.value}
            className={s.exportModalDescription}
          />
          <span>.csv</span>
        </div>
      </ContentBox>
    );
  }
}

export default modal(BulkExportModal);
