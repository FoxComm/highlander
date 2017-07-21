// @flow

import React, { Element } from 'react';
import styles from './arrows.css';

type Props = {
  disabled: boolean,
  onClick: () => {},
};

const GalleryRightArrow = (props: Props) => {
  const { disabled, onClick } = props;

  const handleClick = (event) => {
    event.stopPropagation();
    event.preventDefault();
    onClick(event);
  };

  return (
    <div styleName="right-handle">
      <button
        styleName="right"
        disabled={disabled}
        onClick={handleClick}
      />
    </div>
  );
};

export function renderRightNav(onClick, disabled) {
  return <GalleryRightArrow disabled={disabled} onClick={onClick} />;
}

export default GalleryRightArrow;
