/* @flow */

// libs
import { isEmpty, noop } from 'lodash';
import classNames from 'classnames';
import React, { Component } from 'react';

// components
import { Button, PrimaryButton } from 'components/core/button';
import ButtonWithMenu from 'components/core/button-with-menu';

// styles
import s from './save-cancel.css';

type Props = {
  /** Additional className */
  className?: string;
  /** Component tabindex value */
  cancelTabIndex: string;
  /** Save button tabindex value */
  saveTabIndex: string;
  /** Cancel button label */
  cancelText?: string;
  /** If cancel button is disabled */
  cancelDisabled?: boolean;
  /** Save button label */
  saveText?: string;
  /** If save button is disabled */
  saveDisabled?: boolean;
  /** If provided, save button acts as a ButtonWithMenu - it provides additional actions in a menu */
  saveItems?: SaveComboItems,
  /** Dropdown menu position. Affects animation start position (css's transform-origin). Used when 'saveItems' is not empty  */
  saveMenuPosition?: 'left'|'right',
  /** Callback called on save button click */
  onSave?: (value: any) => void;
  /** Callback called on menu item click. Used when 'saveItems' is not empty  */
  onSaveSelect?: (value: any) => void;
  /** Callback called on cancel button click */
  onCancel?: (event: SyntheticEvent) => void;
  /** If to show loading animation */
  isLoading?: boolean;
  /** If true, wil set focus on Cancel button, otherwise do nothing */
  focusCancel?: boolean;
  /** If true, wil set focus on Action button, otherwise do nothing */
  focusAction?: boolean;
};

/**
 * SaveCancel component implements simple wrapper over 2 components: Save and Cancel
 * It produces custom-handled button or save button depending on whether onSave prop given
 *
 * @class SaveCancel
 */
export default class SaveCancel extends Component {
  props: Props;

  static defaultProps: $Shape<Props> = {
    className: '',
    cancelTabIndex: '101',
    saveTabIndex: '102',
    cancelText: 'Cancel',
    cancelDisabled: false,
    saveText: 'Save',
    saveDisabled: false,
    saveItems: [],
    saveMenuPosition: 'right',
    onSaveSelect: noop,
    onCancel: noop,
    isLoading: false,
  };

  _cancel: HTMLElement;
  _action: HTMLElement;

  componentDidMount() {
    if (this.props.focusCancel && this._cancel) {
      this._cancel.focus();
    } else if (this.props.focusAction && this._action) {
      this._action.focus();
    }
  }

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
        returnRef={r => this._cancel = r}
        onClick={onCancel}
        className={classNames(s.cancel, 'fc-save-cancel__cancel')}
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
        returnRef={r => this._action = r}
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
}
