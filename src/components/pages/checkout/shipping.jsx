
/* @flow */

import React from 'react';
import EditableBlock from '../../editable-block';
import cssModules from 'react-css-modules';
import styles from './checkout.css';

type ShippingProps = {
  isEditing: ?boolean;
};

const Shipping = (props: ShippingProps) => {
  return (
    <EditableBlock
      title="SHIPPING"
      isEditing={props.isEditing}
      viewContent="hello"
      editContent="edit content"
    />
  );
};

export default cssModules(Shipping, styles);
