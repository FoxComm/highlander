// @flow

import React, { Element } from 'react';
import styles from './arrows.css';

type Props = {
  disabled: boolean,
  onClick: () => {},
};

const GalleryLeftArrow = (props: Props) => {
  const { disabled, onClick } = props;
  
  const handleClick = (event) => {
    event.stopPropagation();
    event.preventDefault();
    onClick(event);
  };
  
  return (
    <div styleName="left-handle">
      <button
        styleName="left"
        disabled={disabled}
        onClick={handleClick}
      />
    </div>
  );
};

export function renderLeftNav(onClick, disabled) {
  return <GalleryLeftArrow disabled={disabled} onClick={onClick} />;
}

export default GalleryLeftArrow;
