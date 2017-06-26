/* @flow */

// libs
import isEmpty from 'lodash/isEmpty';
import { autobind } from 'core-decorators';
import React, { Component } from 'react';

// helpers
import { numberize } from 'lib/text-utils';

// components
import ConfirmationModal from 'components/core/confirmation-modal';
import TextInput from 'components/core/text-input';

import s from './modal.css';

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

export default class BulkExportModal extends Component {
  props: Props;
  static defaultProps = {
    inBulk: false,
  };

  state: State = {
    value: '',
  };

  @autobind
  handleChange(value: string) {
    this.setState({ value });
  }

  @autobind
  handleKeyDown(event: KeyboardEvent) {
    const { key } = event;

    if (key === 'Enter') {
      this.handleSave();
    }
  }

  @autobind
  handleSave() {
    const description = !isEmpty(this.state.value) ? this.state.value : null;

    this.props.onConfirm(description);
  }

  get label() {
    const { entity, count } = this.props;
    const entityForm = numberize(entity, count);

    return <span>Are you sure you want to export <b>{count ? count : 'all'} {entityForm}</b>?</span>;
  }

  render() {
    const { inBulk, title, onCancel } = this.props;
    const modalTitle = inBulk ? `Export All ${title}` : `Export Selected ${title}`;
    const fileName = title.toLowerCase().replace(' ', '_');

    return (
      <ConfirmationModal
        title={modalTitle}
        confirmLabel="Yes, Export"
        onConfirm={this.handleSave}
        onCancel={onCancel}
        isVisible
      >
        {this.label}
        <div className={s.exportModalFileTitle}>
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
      </ConfirmationModal>
    );
  }
}
