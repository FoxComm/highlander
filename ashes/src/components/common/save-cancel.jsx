/* @flow */

// libs
import isEmpty from 'lodash/isEmpty';
import React, { Component, PropTypes } from 'react';
import classNames from 'classnames';

// components
import { Button, PrimaryButton } from './buttons';
import ButtonWithMenu from './button-with-menu';

type Props = {
  className?: string;
  cancelTabIndex: string;
  saveTabIndex: string;
  cancelText: string;
  cancelDisabled?: boolean;
  saveText: string;
  saveDisabled?: boolean;
  saveItems?: SaveComboItems,
  saveMenuPosition?: 'left'|'right',
  onSave?: (value: any) => void;
  onSaveSelect?: (value: any) => void;
  onCancel?: (event: SyntheticEvent) => void;
  isLoading?: boolean;
};

/**
 * SaveCancel component implements simple wrapper over 2 components: Save and Cancel
 * It produces custom-handled button or save button depending on whether onSave prop given
 *
 * @class SaveCancel
 */
export default class SaveCancel extends Component {
  props: Props;

  static defaultProps = {
    cancelTabIndex: '0',
    cancelText: 'Cancel',
    cancelParams: {},
    saveTabIndex: '1',
    saveText: 'Save',
  };

  get cancel() {
    const {
      cancelTabIndex,
      onCancel,
      cancelText,
      cancelDisabled,
    } = this.props;

    return (
      <Button
        id="fct-modal-cancel-btn"
        type="button"
        onClick={onCancel}
        className="fc-save-cancel__cancel"
        tabIndex={cancelTabIndex}
        disabled={cancelDisabled}
        children={cancelText}
      />
    );
  }

  get primary() {
    const {
      saveTabIndex,
      saveText,
      saveItems,
      saveMenuPosition,
      saveDisabled,
      onSave,
      onSaveSelect,
      isLoading,
    } = this.props;


    if (!isEmpty(saveItems)) {
      return (
        <ButtonWithMenu
          isLoading={isLoading}
          title={saveText}
          menuPosition={saveMenuPosition}
          items={saveItems}
          onPrimaryClick={onSave}
          onSelect={onSaveSelect}
          buttonDisabled={saveDisabled}
          menuDisabled={saveDisabled}
        />
      );
    }

    return (
      <PrimaryButton
        id="fct-modal-confirm-btn"
        type={onSave ? 'button' : 'submit'}
        onClick={onSave}
        className="fc-save-cancel__save"
        tabIndex={saveTabIndex}
        isLoading={isLoading}
        disabled={saveDisabled}
        children={saveText}
      />
    );
  }

  render() {
    return (
      <div className={classNames('fc-save-cancel', this.props.className)}>
        {this.cancel}
        {this.primary}
      </div>
    );
  }
};
