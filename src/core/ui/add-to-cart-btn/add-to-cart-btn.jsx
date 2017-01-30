/* @flow */

// libs
import React from 'react';
import cx from 'classnames';

// styles
import styles from './add-to-cart-btn.css';

// components
import Icon from 'ui/icon';

type Props = {
  pdp?: bool,
  expanded?: bool,
  onClick?: Function,
  className?: string,
};

const AddToCartBtn = (props: Props) => {
  const { pdp, expanded = false, onClick, className, ...restProps } = props;
  const classNames = cx(className, styles['add-to-cart-btn'], {
    [styles.expanded]: expanded,
  });


  return (
    <button className={classNames} onClick={onClick}>
      <span styleName="add-icon-wrapper">
        <Icon name="fc-add" styleName="add-icon" />
      </span>
      { pdp && <span styleName="pdp-add-btn-title">ADD TO CART</span>}
      { !pdp && <span styleName="add-btn-title">ADD TO CART</span>}
    </button>
  );
};

export default AddToCartBtn;
