/* @flow */

import React from 'react';

import wrapModal from 'components/modal/wrapper';
import type { Props } from './modal-base';
import ModalBase from './modal-base';

const Wrapped: Class<React.Component<void, Props, any>> = wrapModal(ModalBase);

export default Wrapped;
