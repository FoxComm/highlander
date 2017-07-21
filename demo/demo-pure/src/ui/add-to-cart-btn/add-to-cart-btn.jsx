/* @flow */

// libs
import React from 'react';

// components
import Button from 'ui/buttons';

type Props = {
  onClick?: () => void,
  className?: string,
};

const AddToCartBtn = (props: Props) => {
  return (
    <Button {...props}>
      Add to cart
    </Button>
  );
};

export default AddToCartBtn;
