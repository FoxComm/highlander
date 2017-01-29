// @flow

import React, { Element } from 'react';
import { Button } from './buttons';
import ButtonWithMenu from './button-with-menu';

import styles from './save-cancel-with-menu.css';

import type { SaveComboItems } from 'paragons/common';

type Props = {
  isLoading?: boolean,
  cancelDisabled?: boolean,
  cancelTabIndex?: number,
  cancelText?: string,
  onCancel: Function,
  primaryItems: SaveComboItems,
  primaryMenuPosition?: 'left'|'right',
  primaryText?: string,
  onPrimaryClick: Function,
  onPrimarySelect: Function,
};

// SaveCancelWithMenu is a component that implements a simple wrapper over
// the cancel button and the button with menu components.
const SaveCancelWithMenu = ({
  isLoading = false,
  cancelDisabled = false,
  cancelTabIndex = 1,
  cancelText = 'Cancel',
  onCancel,
  primaryItems,
  primaryMenuPosition = 'right',
  primaryText = 'Save',
  onPrimaryClick,
  onPrimarySelect,
}: Props): Element => {
  return (
    <div>
      <Button
        styleName="cancel-btn"
        type="button"
        onClick={onCancel}
        tabIndex={cancelTabIndex}
        disabled={cancelDisabled || isLoading}
      >
        {cancelText}
      </Button>
      <ButtonWithMenu
        isLoading={isLoading}
        title={primaryText}
        menuPosition={primaryMenuPosition}
        items={primaryItems}
        onPrimaryClick={onPrimaryClick}
        onSelect={onPrimarySelect}
      />
    </div>
  );
};

export default SaveCancelWithMenu;
